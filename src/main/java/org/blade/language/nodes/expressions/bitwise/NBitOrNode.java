package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BladeRuntimeError;

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
    throw BladeRuntimeError.argumentError(this,"|", left, right);
  }
}
