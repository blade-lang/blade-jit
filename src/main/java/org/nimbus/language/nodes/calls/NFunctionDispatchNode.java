package org.nimbus.language.nodes.calls;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import org.nimbus.language.runtime.NFunctionObject;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

//@NodeField(name = "profile", type = BranchProfile.class)
@SuppressWarnings("truffle-inlining")
public abstract class NFunctionDispatchNode extends Node {
  public abstract Object executeDispatch(Object function, Object[] values);
//  public abstract BranchProfile getProfile();

  @ExplodeLoop
  private static Object[] extendArguments(Object[] arguments, NFunctionObject function) {
    if (arguments.length >= function.argumentsCount && function.methodTarget == null) {
      return arguments;
    }

    int totalLength = function.argumentsCount;
    Object[] ret;

    int start = 0;
    if(function.methodTarget != null) {
      totalLength += 1;
      ret = new Object[totalLength];
      ret[0] = function.methodTarget;
      start = 1;
    } else {
      ret = new Object[totalLength];
    }

    for (int i = start; i < totalLength; i++) {
      ret[i] = i < arguments.length ? arguments[i] : NimNil.SINGLETON;
    }

    return ret;
  }

  @Specialization(guards = "function.callTarget == callNode.getCallTarget()", limit = "3")
  protected Object directDispatch(
    NFunctionObject function, Object[] arguments,
    @Cached("function") NFunctionObject cachedFunction,
    @Cached("create(function.callTarget)") DirectCallNode callNode
  ) {
    if(cachedFunction.argumentsCount > arguments.length) {
//      getProfile().enter();
      return callNode.call(extendArguments(arguments, cachedFunction));
    }
    return callNode.call(arguments);
  }

  @Specialization(replaces = "directDispatch")
  protected Object indirectDispatch(
    NFunctionObject function, Object[] arguments,
    @Cached("function") NFunctionObject cachedFunction,
    @Cached IndirectCallNode callNode
  ) {
    return callNode.call(cachedFunction.callTarget, extendArguments(arguments, cachedFunction));
  }

  @Fallback
  protected Object invalidFunctionCall(Object object, Object[] arguments) {
    throw new NimRuntimeError("cannot call non-function '" + object + "'");
  }
}
