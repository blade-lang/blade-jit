package org.zuri.language.nodes.functions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import org.zuri.language.runtime.ZuriNil;
import org.zuri.language.runtime.ZuriRuntimeError;
import org.zuri.language.runtime.FunctionObject;

@SuppressWarnings("truffle-inlining")
public abstract class NMethodDispatchNode extends Node {
  public abstract Object executeDispatch(Object function, Object receiver, Object[] values);

  @Specialization(guards = "function.getCallTarget() == callNode.getCallTarget()", limit = "3")
  protected Object directDispatch(
    FunctionObject function, Object receiver, Object[] arguments,
    @Cached("function") FunctionObject cachedFunction,
    @Cached("create(function.callTarget)") DirectCallNode callNode
  ) {
    return callNode.call(extendArguments(cachedFunction, receiver, arguments));
  }

  @Specialization(replaces = "directDispatch")
  protected Object indirectDispatch(
    FunctionObject function, Object receiver, Object[] arguments,
    @Cached("function") FunctionObject cachedFunction,
    @Cached IndirectCallNode callNode
  ) {
    return callNode.call(cachedFunction.callTarget, extendArguments(cachedFunction, receiver, arguments));
  }

  @Fallback
  protected Object invalidFunctionCall(Object object, Object receiver, Object[] arguments) {
    throw ZuriRuntimeError.error(this, "cannot call non-function '", object, "'");
  }

  @ExplodeLoop
  private Object[] extendArguments(FunctionObject function, Object receiver, Object[] arguments) {
    int finalLength = function.argumentsCount + 1;
    int argumentLength = arguments.length;

    Object[] ret = new Object[finalLength];
    ret[0] = receiver;

    for (int i = 1; i < finalLength; i++) {
      int j = i - 1;
      ret[i] = j < argumentLength ? arguments[j] : ZuriNil.SINGLETON;
    }

    return ret;
  }
}
