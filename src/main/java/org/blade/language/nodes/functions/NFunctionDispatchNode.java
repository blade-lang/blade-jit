package org.blade.language.nodes.functions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.runtime.FunctionObject;
import org.blade.language.runtime.BladeRuntimeError;

@SuppressWarnings("truffle-inlining")
public abstract class NFunctionDispatchNode extends Node {
  public abstract Object executeDispatch(Object function, Object[] values);

  @Specialization(guards = "function.callTarget == callNode.getCallTarget()", limit = "3")
  protected Object directDispatch(
    FunctionObject function, Object[] arguments,
    @Cached("create(function.callTarget)") DirectCallNode callNode
  ) {
    return callNode.call(arguments);
  }

  @Specialization(replaces = "directDispatch")
  protected Object indirectDispatch(
    FunctionObject function, Object[] arguments,
    @Cached("function") FunctionObject cachedFunction,
    @Cached IndirectCallNode callNode
  ) {
    return callNode.call(cachedFunction.callTarget, arguments);
  }

  @Fallback
  protected Object invalidFunctionCall(Object object, Object[] arguments) {
    throw BladeRuntimeError.create("cannot call non-function '", object, "'");
  }
}
