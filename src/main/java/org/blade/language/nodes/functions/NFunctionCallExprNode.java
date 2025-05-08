package org.blade.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.*;

import java.util.List;

public abstract class NFunctionCallExprNode extends NNode {

  @Children
  protected final NNode[] arguments;
  @CompilerDirectives.CompilationFinal
  protected final int argsMinus1;
  @SuppressWarnings("FieldMayBeFinal")
  @Executed
  @Child
  protected NNode target;
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
                                      @Cached(value = "languageContext()", neverDefault = false) @Cached.Shared("group") BladeContext context) {
    return dispatchNode.executeDispatch(cachedFunction, expandLessVarArguments(context, cachedFunction, consumeArguments(frame)));
  }

  @Specialization(guards = {"function.variadic", "arguments.length >= function.argumentsCount", "function.argumentsCount > 1"})
  protected Object doVariableMoreSize(VirtualFrame frame, FunctionObject function,
                                      @Cached("function") FunctionObject cachedFunction,
                                      @Cached(value = "languageContext()", neverDefault = false) @Cached.Shared("group") BladeContext context) {
    return dispatchNode.executeDispatch(cachedFunction, expandMoreVarArguments(context, cachedFunction, consumeArguments(frame)));
  }

  @Specialization(guards = {"function.variadic", "arguments.length >= function.argumentsCount", "function.argumentsCount == 1"})
  protected Object doVariableNoSize(VirtualFrame frame, FunctionObject function,
                                    @Cached("function") FunctionObject cachedFunction,
                                    @Cached(value = "languageContext()", neverDefault = false) @Cached.Shared("group") BladeContext context) {
    return dispatchNode.executeDispatch(cachedFunction, expandNoVarArguments(context, cachedFunction, consumeArguments(frame)));
  }

  @Specialization(replaces = "doSameSize")
  protected Object doNotSameSize(VirtualFrame frame, FunctionObject function,
                                 @Cached("function") FunctionObject cachedFunction) {
    return dispatchNode.executeDispatch(cachedFunction, extendArguments(cachedFunction, consumeArguments(frame)));
  }

  @Fallback
  protected Object instantiateNonConstructor(VirtualFrame frame, Object object) {
    consumeArguments(frame);
    throw BladeRuntimeError.create("'", object, "' is not a callable function");
  }

  @ExplodeLoop
  private Object[] consumeArguments(VirtualFrame frame) {
    Object[] values = new Object[arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      values[i] = arguments[i].execute(frame);
    }
    return values;
  }

  @ExplodeLoop
  private Object[] extendArguments(FunctionObject function, Object[] arguments) {
    Object[] ret = new Object[function.argumentsCount];

    if (arguments.length > 0) {
      System.arraycopy(arguments, 0, ret, 0, arguments.length);
    }

    for (int i = arguments.length; i < function.argumentsCount; i++) {
      ret[i] = BladeNil.SINGLETON;
    }

    return ret;
  }

  /**
   * Specially used for variadic functions
   */
  @ExplodeLoop
  private Object[] expandLessVarArguments(BladeContext context, FunctionObject function, Object[] arguments) {
    int finalLength = function.argumentsCount + 1;
    Object[] ret = new Object[finalLength];

    if (arguments.length > 0) {
      System.arraycopy(arguments, 1, ret, 1, arguments.length - 1);
    }

    for (int i = arguments.length; i < function.argumentsCount; i++) {
      ret[i] = BladeNil.SINGLETON;
    }

    ret[function.argumentsCount] = new ListObject(
      context.objectsModel.listShape,
      context.objectsModel.listObject,
      new Object[0]
    );

    return ret;
  }

  /**
   * Specially used for variadic functions
   */
  @ExplodeLoop
  private Object[] expandMoreVarArguments(BladeContext context, FunctionObject function, Object[] arguments) {
    int finalLength = function.argumentsCount + 1;
    Object[] ret = new Object[finalLength];

    System.arraycopy(arguments, 1, ret, 1, function.argumentsCount - 1);

    int varLength = arguments.length - function.argumentsCount;
    Object[] variadic = new Object[varLength];
    System.arraycopy(arguments, function.argumentsCount, variadic, 0, varLength);

    ret[function.argumentsCount] = new ListObject(
      context.objectsModel.listShape,
      context.objectsModel.listObject,
      variadic
    );

    return ret;
  }

  /**
   * Specially used for variadic functions
   */
  @ExplodeLoop
  private Object[] expandNoVarArguments(BladeContext context, FunctionObject function, Object[] arguments) {
    Object[] ret = new Object[2];

    Object[] variadic = new Object[arguments.length - 1];
    System.arraycopy(arguments, 1, variadic, 0, arguments.length - 1);

    ret[1] = new ListObject(
      context.objectsModel.listShape,
      context.objectsModel.listObject,
      variadic
    );

    return ret;
  }
}
