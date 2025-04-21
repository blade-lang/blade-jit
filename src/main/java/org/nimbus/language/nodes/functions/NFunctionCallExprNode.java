package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NFunctionObject;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

import java.util.List;

public abstract class NFunctionCallExprNode extends NNode {

  @SuppressWarnings("FieldMayBeFinal")
  @Executed
  @Child protected NNode target;

  @Children
  protected final NNode[] arguments;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NFunctionDispatchNode dispatchNode = NFunctionDispatchNodeGen.create();

  @CompilerDirectives.CompilationFinal
  protected final int argsPlus1;

  public NFunctionCallExprNode(NNode target, List<NNode> arguments) {
    this.target = target;
    this.arguments = arguments.toArray(new NNode[0]);
    argsPlus1 = this.arguments.length + 1;
  }

  @Specialization(guards = "function.argumentsCount == argsPlus1")
  protected Object doSameSize(VirtualFrame frame, NFunctionObject function) {
    return dispatchNode.executeDispatch(function, consumeArguments(frame));
  }

  @Specialization(replaces = "doSameSize")
  protected Object doNotSameSize(VirtualFrame frame, NFunctionObject function) {
    return dispatchNode.executeDispatch(function, extendArguments(function, consumeArguments(frame)));
  }

  @Fallback
  protected Object instantiateNonConstructor(VirtualFrame frame, Object object) {
    consumeArguments(frame);
    throw new NimRuntimeError("'" + object + "' is not a callable function");
  }

  @ExplodeLoop
  private Object[] consumeArguments(VirtualFrame frame) {
    Object[] values = new Object[arguments.length];
    for(int i = 0; i < arguments.length; i++) {
      values[i] = arguments[i].execute(frame);
    }
    return values;
  }

  @ExplodeLoop
  private Object[] extendArguments(NFunctionObject function, Object[] arguments) {
    int finalLength = function.argumentsCount + 1;
    Object[] ret = new Object[finalLength];

    if (arguments.length > 0) {
      System.arraycopy(arguments, 0, ret, 0, arguments.length);
    }

    for(int i = arguments.length; i < finalLength; i++) {
      ret[i] = NimNil.SINGLETON;
    }

    return ret;
  }
}
