package org.blade.language.nodes.functions;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.bytecode.Variadic;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import org.blade.language.nodes.list.NReadListIndexNode;
import org.blade.language.nodes.list.NReadListIndexNodeGen;
import org.blade.language.runtime.*;

@ImportStatic(BladeContext.class)
public abstract class NDefCallNode extends Node {

  public static NDefCallNode getUncached() {
    return NDefCallNodeGen.getUncached();
  }

  @NeverDefault
  public static NDefCallNode create() {
    return NDefCallNodeGen.create();
  }

  public abstract Object executeCall(int argumentLength, Object function, Object[] arguments);

  @Specialization(guards = {"function.getArgumentsCount() == argumentsLength", "!function.isVariadic()"}, assumptions = "callTargetStable")
  public static Object doSameSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                  @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                  @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                  @Cached("create(cachedTarget)") DirectCallNode callNode) {
    return callNode.call(arguments);
  }

  @Specialization(guards = {"function.isVariadic()", "argumentsLength < function.getArgumentsCount()"}, assumptions = "callTargetStable")
  public static Object doVariableLessSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                          @Bind Node node,
                                          @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                          @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                          @Cached("create(cachedTarget)") DirectCallNode callNode,
                                          @Cached(value = "get(node)") BladeContext context,
                                          @Cached("context.objectsModel.listShape") Shape listShape,
                                          @Cached("context.objectsModel.listObject") BladeClass listClass) {
    return callNode.call(expandLessVarArguments(
      arguments,
      function.getArgumentsCount(),
      argumentsLength,
      listShape,
      listClass
    ));
  }

  @Specialization(guards = {"function.isVariadic()", "argumentsLength >= function.getArgumentsCount()", "function.getArgumentsCount() > 1"}, assumptions = "callTargetStable")
  public static Object doVariableMoreSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                          @Bind Node node,
                                          @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                          @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                          @Cached("create(cachedTarget)") DirectCallNode callNode,
                                          @Cached(value = "get(node)") BladeContext context,
                                          @Cached("context.objectsModel.listShape") Shape listShape,
                                          @Cached("context.objectsModel.listObject") BladeClass listClass) {
    return callNode.call(expandMoreVarArguments(
      arguments,
      function.getArgumentsCount(),
      argumentsLength,
      listShape,
      listClass
    ));
  }

  @Specialization(guards = {"function.isVariadic()", "arguments.length >= function.getArgumentsCount()", "function.getArgumentsCount() == 1"}, assumptions = "callTargetStable")
  public static Object doVariableNoSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                        @Bind Node node,
                                        @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                        @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                        @Cached("create(cachedTarget)") DirectCallNode callNode,
                                        @Cached(value = "get(node)") BladeContext context,
                                        @Cached("context.objectsModel.listShape") Shape listShape,
                                        @Cached("context.objectsModel.listObject") BladeClass listClass) {
    return callNode.call(expandNoVarArguments(arguments, argumentsLength, listShape, listClass));
  }

  @Specialization(replaces = "doSameSize")
  public static Object doNotSameSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                     @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                     @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                     @Cached("create(cachedTarget)") DirectCallNode callNode) {
    return callNode.call(extendArguments(arguments, function.getArgumentsCount() + 1));
  }

  @Specialization
  public static Object doInterop(int argumentsLength, Object function, @Variadic Object[] arguments,
                                 @CachedLibrary(limit = "3") InteropLibrary library, @Bind Node node) {
    try {
      return library.execute(function, arguments);
    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
      throw BladeRuntimeError.error(node, "'", function, "' is not a callable function");
    }
  }

  @ExplodeLoop
  private static Object[] extendArguments(Object[] arguments, int finalLength) {
    int argumentLength = arguments.length;

    Object[] ret = new Object[finalLength];

    if (argumentLength > 0) {
      System.arraycopy(arguments, 1, ret, 1, argumentLength - 1);
    }

    for (int i = argumentLength; i < finalLength; i++) {
      ret[i] = BladeNil.SINGLETON;
    }

    return ret;
  }

  // Specially used for variadic functions
  @ExplodeLoop
  private static Object[] expandLessVarArguments(Object[] arguments, int functionArity, int argumentsLength, Shape listShape, BladeClass listClass) {
    int argumentLength = arguments.length;

    int finalLength = functionArity + 1;
    Object[] ret = new Object[finalLength];

    if (argumentLength > 0) {
      System.arraycopy(arguments, 1, ret, 1, argumentLength - 1);
    }

    for (int i = argumentLength; i < functionArity; i++) {
      ret[i] = BladeNil.SINGLETON;
    }

    ret[functionArity] = new ListObject(
      listShape,
      listClass,
      new Object[0]
    );

    return ret;
  }

  // Specially used for variadic functions
  @ExplodeLoop
  private static Object[] expandMoreVarArguments(Object[] arguments, int functionArity, int argumentsLength, Shape listShape, BladeClass listClass) {
    int finalLength = functionArity + 1;
    Object[] ret = new Object[finalLength];

    System.arraycopy(arguments, 1, ret, 1, functionArity - 1);

    int varLength = argumentsLength - functionArity;
    Object[] variadic = new Object[varLength];
    System.arraycopy(arguments, functionArity, variadic, 0, varLength);

    ret[functionArity] = new ListObject(
      listShape,
      listClass,
      variadic
    );

    return ret;
  }

  // Specially used for variadic functions
  @ExplodeLoop
  private static Object[] expandNoVarArguments(Object[] arguments, int argumentsLength, Shape listShape, BladeClass listClass) {
    Object[] ret = new Object[2];

    Object[] variadic = new Object[argumentsLength - 1];
    System.arraycopy(arguments, 1, variadic, 0, argumentsLength - 1);

    ret[1] = new ListObject(
      listShape,
      listClass,
      variadic
    );

    return ret;
  }
}
