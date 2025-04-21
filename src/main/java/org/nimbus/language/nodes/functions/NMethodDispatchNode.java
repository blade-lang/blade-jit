package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import org.nimbus.language.runtime.NFunctionObject;
import org.nimbus.language.runtime.NString;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

@SuppressWarnings("truffle-inlining")
public abstract class NMethodDispatchNode extends Node {
  public abstract Object executeDispatch(Object function, Object receiver, Object[] values);

  @Specialization(guards = "function.callTarget == callNode.getCallTarget()", limit = "3")
  protected Object directDispatch(
    NFunctionObject function,  Object receiver, Object[] arguments,
    @Cached("receiver") Object cachedReceiver,
    @Cached("function") NFunctionObject cachedFunction,
    @Cached("create(function.callTarget)") DirectCallNode callNode
  ) {
    return callNode.call(extendArguments(cachedFunction, cachedReceiver, arguments));
  }

  @Specialization(replaces = "directDispatch")
  protected Object indirectDispatch(
    NFunctionObject function,  Object receiver, Object[] arguments,
    @Cached(value = "receiver", neverDefault = false) Object cachedReceiver,
    @Cached("function") NFunctionObject cachedFunction,
    @Cached IndirectCallNode callNode
  ) {
    return callNode.call(cachedFunction.callTarget, extendArguments(cachedFunction, cachedReceiver, arguments));
  }

  @Fallback
  protected Object invalidFunctionCall(Object object, Object receiver, Object[] arguments) {
    throw NimRuntimeError.create("cannot call non-function '", object, "'");
  }

  @ExplodeLoop
  private Object[] extendArguments(NFunctionObject function, Object receiver, Object[] arguments) {
    int finalLength = function.argumentsCount + 1;
    Object[] ret = new Object[finalLength];
    ret[0] = receiver;

    if (arguments.length >= 1) {
      System.arraycopy(arguments, 0, ret, 1, arguments.length);
    }

    for(int i = arguments.length + 1; i < finalLength; i++) {
      ret[i] = NimNil.SINGLETON;
    }

    return ret;
  }
}
