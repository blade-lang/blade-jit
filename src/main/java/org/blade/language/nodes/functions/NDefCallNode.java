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

@GenerateInline
@ImportStatic(BladeContext.class)
public abstract class NDefCallNode extends Node {

  @NeverDefault
  public static NDefCallNode create() {
    return NDefCallNodeGen.create();
  }

  public abstract Object executeCall(Node node, int argumentLength, Object function, Object[] arguments);

  @Specialization(guards = {"function.getArgumentsCount() == argumentsLength", "!function.isVariadic()"}, assumptions = "callTargetStable")
  public static Object doSameSize(Node node, int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                  @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                  @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                  @Cached("create(cachedTarget)") DirectCallNode callNode) {
    return callNode.call(arguments);
  }

  @Specialization(guards = {"function.isVariadic()", "argumentsLength < function.getArgumentsCount()"}, assumptions = "callTargetStable")
  public static Object doVariableLessSize(Node node, int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
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
  public static Object doVariableMoreSize(Node node, int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
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
  public static Object doVariableNoSize(Node node, int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                        @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                        @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                        @Cached("create(cachedTarget)") DirectCallNode callNode,
                                        @Cached(value = "get(node)") BladeContext context,
                                        @Cached("context.objectsModel.listShape") Shape listShape,
                                        @Cached("context.objectsModel.listObject") BladeClass listClass) {
    return callNode.call(expandNoVarArguments(arguments, argumentsLength, listShape, listClass));
  }

  @Specialization(replaces = "doSameSize")
  public static Object doNotSameSize(Node node, int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                     @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                     @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                     @Cached("create(cachedTarget)") DirectCallNode callNode) {
    return callNode.call(extendArguments(arguments, argumentsLength, function.getArgumentsCount()));
  }

  @Specialization
  public static Object doInterop(Node node, int argumentsLength, Object function, @Variadic Object[] arguments,
                                 @CachedLibrary(limit = "3") InteropLibrary library) {
    try {
      return library.execute(function, arguments);
    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
      throw BladeRuntimeError.error(node, "'", function, "' is not a callable function");
    }
  }

  @ExplodeLoop
  private static Object[] extendArguments(Object[] arguments, int argumentLength, int finalLength) {
    Object[] ret = new Object[finalLength];

    if (argumentLength > 0) {
      System.arraycopy(arguments, 0, ret, 0, Math.min(argumentLength, finalLength));
    }

    for (int i = argumentLength; i < finalLength; i++) {
      ret[i] = BladeNil.SINGLETON;
    }

    return ret;
  }

  // Specially used for variadic functions
  @ExplodeLoop
  private static Object[] expandLessVarArguments(Object[] arguments, int functionArity, int argumentLength, Shape listShape, BladeClass listClass) {
    int nonVariadicLength = functionArity - 1;
    Object[] ret = new Object[functionArity];

    if (argumentLength > 0) {
      System.arraycopy(arguments, 0, ret, 0, argumentLength);
    }

    for (int i = argumentLength; i < nonVariadicLength; i++) {
      ret[i] = BladeNil.SINGLETON;
    }

    ret[nonVariadicLength] = new ListObject(
      listShape,
      listClass,
      new Object[0]
    );

    return ret;
  }

  // Specially used for variadic functions
  @ExplodeLoop
  private static Object[] expandMoreVarArguments(Object[] arguments, int functionArity, int argumentsLength, Shape listShape, BladeClass listClass) {
    int nonVariadicLength = functionArity - 1;
    Object[] ret = new Object[functionArity];

    System.arraycopy(arguments, 0, ret, 0, nonVariadicLength);

    int varLength = argumentsLength - functionArity;
    Object[] variadic = new Object[varLength];
    System.arraycopy(arguments, nonVariadicLength, variadic, 0, varLength);

    ret[nonVariadicLength] = new ListObject(
      listShape,
      listClass,
      variadic
    );

    return ret;
  }

  // Specially used for variadic functions
  @ExplodeLoop
  private static Object[] expandNoVarArguments(Object[] arguments, int argumentsLength, Shape listShape, BladeClass listClass) {
    Object[] ret = new Object[1];

    Object[] variadic = new Object[argumentsLength];
    System.arraycopy(arguments, 0, variadic, 0, argumentsLength);

    ret[0] = new ListObject(
      listShape,
      listClass,
      variadic
    );

    return ret;
  }
}
