package org.blade.language.nodes.functions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.runtime.FunctionObject;
import org.blade.language.runtime.BladeNil;
import org.blade.language.runtime.BladeRuntimeError;

@SuppressWarnings("truffle-inlining")
public abstract class NMethodDispatchNode extends Node {
  public abstract Object executeDispatch(Object function, Object receiver, Object[] values);

  @Specialization(guards = "function.callTarget == callNode.getCallTarget()", limit = "3")
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
    throw BladeRuntimeError.create("cannot call non-function '", object, "'");
  }

  @ExplodeLoop
  private Object[] extendArguments(FunctionObject function, Object receiver, Object[] arguments) {
    int finalLength = function.argumentsCount + 1;
    int argumentLength = arguments.length;

    Object[] ret = new Object[finalLength];
    ret[0] = receiver;

    for(int i = 1; i < finalLength; i++) {
      int j = i - 1;
      ret[i] = j < argumentLength ? arguments[j] : BladeNil.SINGLETON;
    }

    return ret;
  }
}
