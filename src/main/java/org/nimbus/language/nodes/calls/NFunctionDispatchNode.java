package org.nimbus.language.nodes.calls;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.nimbus.language.runtime.NFunctionObject;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

@SuppressWarnings("truffle-inlining")
public abstract class NFunctionDispatchNode extends Node {
  public abstract Object executeDispatch(Object function, Object[] values);

  @Specialization(guards = "function.callTarget == callNode.getCallTarget()", limit = "3")
  protected Object directDispatch(
    NFunctionObject function, Object[] arguments,
    @Cached("function") NFunctionObject cachedFunction,
    @Cached("create(function.callTarget)") DirectCallNode callNode
  ) {
    return callNode.call(arguments);
  }

  @Specialization(replaces = "directDispatch")
  protected Object indirectDispatch(
    NFunctionObject function, Object[] arguments,
    @Cached("function") NFunctionObject cachedFunction,
    @Cached IndirectCallNode callNode
  ) {
    return callNode.call(cachedFunction.callTarget, arguments);
  }

  @Fallback
  protected Object invalidFunctionCall(Object object, Object[] arguments) {
    throw new NimRuntimeError("cannot call non-function '" + object + "'");
  }
}
