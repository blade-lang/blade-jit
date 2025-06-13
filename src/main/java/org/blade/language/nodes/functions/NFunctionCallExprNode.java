package org.blade.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.Shape;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.*;

import java.util.List;

public abstract class NFunctionCallExprNode extends NNode {

  @CompilerDirectives.CompilationFinal
  protected final int argsMinus1;

  @SuppressWarnings("FieldMayBeFinal")
  @Executed
  @Child
  protected NNode target;

  @Children
  protected final NNode[] arguments;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NFunctionDispatchNode dispatchNode = NFunctionDispatchNodeGen.create();

  public NFunctionCallExprNode(NNode target, List<NNode> arguments) {
    this.target = target;
    this.arguments = arguments.toArray(new NNode[0]);
    argsMinus1 = this.arguments.length - 1;
  }

  @Specialization(guards = {"function.argumentsCount == argsMinus1", "!function.variadic"})
  protected Object doSameSize(VirtualFrame frame, FunctionObject function,
                              @Cached("function") FunctionObject cachedFunction) {
    return dispatchNode.executeDispatch(cachedFunction, consumeArguments(frame));
  }

  @Specialization(guards = {"function.variadic", "arguments.length < function.argumentsCount"})
  protected Object doVariableLessSize(VirtualFrame frame, FunctionObject function,
                                      @Cached("function") FunctionObject cachedFunction,
                                      @Cached(value = "languageContext()", neverDefault = false) @Cached.Shared("group") BladeContext context,
                                      @Cached("context.objectsModel.listShape") Shape listShape,
                                      @Cached("context.objectsModel.listObject") BladeClass listClass) {
    return dispatchNode.executeDispatch(
      cachedFunction,
      expandLessVarArguments(cachedFunction, consumeArguments(frame), listShape, listClass)
    );
  }

  @Specialization(guards = {"function.variadic", "arguments.length >= function.argumentsCount", "function.argumentsCount > 1"})
  protected Object doVariableMoreSize(VirtualFrame frame, FunctionObject function,
                                      @Cached("function") FunctionObject cachedFunction,
                                      @Cached(value = "languageContext()", neverDefault = false) @Cached.Shared("group") BladeContext context,
                                      @Cached("context.objectsModel.listShape") Shape listShape,
                                      @Cached("context.objectsModel.listObject") BladeClass listClass) {
    return dispatchNode.executeDispatch(
      cachedFunction,
      expandMoreVarArguments(cachedFunction, consumeArguments(frame), listShape, listClass)
    );
  }

  @Specialization(guards = {"function.variadic", "arguments.length >= function.argumentsCount", "function.argumentsCount == 1"})
  protected Object doVariableNoSize(VirtualFrame frame, FunctionObject function,
                                    @Cached("function") FunctionObject cachedFunction,
                                    @Cached(value = "languageContext()", neverDefault = false) @Cached.Shared("group") BladeContext context,
                                    @Cached("context.objectsModel.listShape") Shape listShape,
                                    @Cached("context.objectsModel.listObject") BladeClass listClass) {
    return dispatchNode.executeDispatch(
      cachedFunction,
      expandNoVarArguments(cachedFunction, consumeArguments(frame), listShape, listClass)
    );
  }

  @Specialization(replaces = "doSameSize")
  protected Object doNotSameSize(VirtualFrame frame, FunctionObject function,
                                 @Cached("function") FunctionObject cachedFunction) {
    return dispatchNode.executeDispatch(cachedFunction, extendArguments(cachedFunction, consumeArguments(frame)));
  }

  @Fallback
  protected Object instantiateNonConstructor(VirtualFrame frame, Object object) {
    consumeArguments(frame);
    throw BladeRuntimeError.error(this, "'", object, "' is not a callable function");
  }

  @ExplodeLoop
  private Object[] consumeArguments(VirtualFrame frame) {
    int length = arguments.length;
    Object[] values = new Object[length];
    for (int i = 0; i < length; i++) {
      values[i] = arguments[i].execute(frame);
    }
    return values;
  }

  @ExplodeLoop
  private Object[] extendArguments(FunctionObject function, Object[] arguments) {
    int finalLength = function.argumentsCount + 1;
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

  /**
   * Specially used for variadic functions
   */
  @ExplodeLoop
  private Object[] expandLessVarArguments(FunctionObject function, Object[] arguments, Shape listShape, BladeClass listClass) {
    int functionArity = function.argumentsCount;
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

  /**
   * Specially used for variadic functions
   */
  @ExplodeLoop
  private Object[] expandMoreVarArguments(FunctionObject function, Object[] arguments, Shape listShape, BladeClass listClass) {
    int functionArity = function.argumentsCount;
    int finalLength = functionArity + 1;
    Object[] ret = new Object[finalLength];

    System.arraycopy(arguments, 1, ret, 1, functionArity - 1);

    int varLength = arguments.length - functionArity;
    Object[] variadic = new Object[varLength];
    System.arraycopy(arguments, functionArity, variadic, 0, varLength);

    ret[functionArity] = new ListObject(
      listShape,
      listClass,
      variadic
    );

    return ret;
  }

  /**
   * Specially used for variadic functions
   */
  @ExplodeLoop
  private Object[] expandNoVarArguments(FunctionObject function, Object[] arguments, Shape listShape, BladeClass listClass) {
    Object[] ret = new Object[2];
    int argumentLength = arguments.length;

    Object[] variadic = new Object[argumentLength - 1];
    System.arraycopy(arguments, 1, variadic, 0, argumentLength - 1);

    ret[1] = new ListObject(
      listShape,
      listClass,
      variadic
    );

    return ret;
  }
}
