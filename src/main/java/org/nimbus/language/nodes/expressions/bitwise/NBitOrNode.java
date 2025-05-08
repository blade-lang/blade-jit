package org.nimbus.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NBitOrNode extends NBinaryNode {

  @Specialization
  protected int doInts(int left, int right) {
    return left | right;
  }

  @Specialization(replaces = "doInts")
  protected int doDoubles(double left, double right) {
    return (int)left | (int)right;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"|", left, right);
  }
}
