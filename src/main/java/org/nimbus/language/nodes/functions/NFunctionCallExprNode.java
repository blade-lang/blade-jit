package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.*;

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
  protected Object doSameSize(VirtualFrame frame, NFunctionObject function,
                              @Cached("function") NFunctionObject cachedFunction) {
    return dispatchNode.executeDispatch(cachedFunction, consumeArguments(frame));
  }

  @Specialization(guards = {"function.variadic"})
  protected Object doVariableSize(VirtualFrame frame, NFunctionObject function,
                                  @Cached("function") NFunctionObject cachedFunction) {
    return dispatchNode.executeDispatch(cachedFunction, expandVarArguments(cachedFunction, consumeArguments(frame)));
  }

  @Specialization(replaces = "doSameSize")
  protected Object doNotSameSize(VirtualFrame frame, NFunctionObject function,
                                 @Cached("function") NFunctionObject cachedFunction) {
    System.out.print(cachedFunction.variadic);
    return dispatchNode.executeDispatch(cachedFunction, extendArguments(cachedFunction, consumeArguments(frame)));
  }

  @Fallback
  protected Object instantiateNonConstructor(VirtualFrame frame, Object object) {
    consumeArguments(frame);
    throw NimRuntimeError.create("'", object, "' is not a callable function");
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
  private Object[] extendArguments(NFunctionObject function, Object[] arguments) {
    Object[] ret = new Object[function.argumentsCount];

    if (arguments.length > 0) {
      System.arraycopy(arguments, 0, ret, 0, arguments.length);
    }

    for (int i = arguments.length; i < function.argumentsCount; i++) {
      ret[i] = NimNil.SINGLETON;
    }

    return ret;
  }

  /**
   * Specially used for variadic functions
   */
  @ExplodeLoop
  private Object[] expandVarArguments(NFunctionObject function, Object[] arguments) {
    Object[] ret = new Object[function.argumentsCount + 1];
    NimContext context = NimContext.get(this);

    if (arguments.length < function.argumentsCount) {
      if (arguments.length > 0) {
        System.arraycopy(arguments, 0, ret, 0, arguments.length);
      }

      for (int i = arguments.length; i < function.argumentsCount - 1; i++) {
        ret[i] = NimNil.SINGLETON;
      }

      ret[function.argumentsCount - 1] = new NListObject(
        context.objectsModel.listShape,
        context.objectsModel.listObject,
        new Object[0]
      );
    } else if(function.argumentsCount > 0) {
      System.arraycopy(arguments, 0, ret, 0, function.argumentsCount - 1);

      Object[] variadics = new Object[arguments.length - function.argumentsCount + 1];
      for (int i = function.argumentsCount - 1; i < arguments.length; i++) {
        variadics[i] = NimNil.SINGLETON;
      }

      ret[function.argumentsCount - 1] = new NListObject(
        context.objectsModel.listShape,
        context.objectsModel.listObject,
        variadics
      );
    } else {
      ret[0] = new NListObject(
        context.objectsModel.listShape,
        context.objectsModel.listObject,
        arguments
      );
    }

    return ret;
  }
}
