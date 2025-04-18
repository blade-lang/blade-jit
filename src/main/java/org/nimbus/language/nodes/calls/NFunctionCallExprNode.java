package org.nimbus.language.nodes.calls;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
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
  @Child private NFunctionDispatchNode dispatchNode;

  public NFunctionCallExprNode(NNode target, List<NNode> arguments) {
    this.target = target;
    this.arguments = arguments.toArray(new NNode[0]);
    dispatchNode = NFunctionDispatchNodeGen.create();
  }

  @Specialization(guards = "arguments.length == function.argumentsCount")
  protected Object doSameSize(VirtualFrame frame, NFunctionObject function) {
    return dispatchNode.executeDispatch(function, consumeArguments(frame));
  }

  @Specialization(replaces = "doSameSize")
  protected Object doNotSameSize(VirtualFrame frame, NFunctionObject function) {
    return dispatchNode.executeDispatch(function, extendArguments(consumeArguments(frame), function));
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
  private Object[] extendArguments(Object[] arguments, NFunctionObject function) {
    if (arguments.length >= function.argumentsCount && function.methodTarget == null) {
      return arguments;
    }

    int start = function.methodTarget == null ? 0 : 1;
    Object[] ret = new Object[function.argumentsCount];
    if(start == 1) {
      ret[0] = function.methodTarget;
    }

    if (arguments.length - start >= 0) {
      System.arraycopy(arguments, 0, ret, start, arguments.length);
    }

    for(int i = arguments.length + start; i < function.argumentsCount; i++) {
      ret[i] = NimNil.SINGLETON;
    }

    return ret;
  }

  /*@ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    Object function = target.execute(frame);

    CompilerAsserts.compilationConstant(arguments.length);

    Object[] values = new Object[arguments.length];
    for(int i = 0; i < arguments.length; i++) {
      values[i] = arguments[i].execute(frame);
    }

    return dispatchNode.executeDispatch(function, values);
  }*/
}
