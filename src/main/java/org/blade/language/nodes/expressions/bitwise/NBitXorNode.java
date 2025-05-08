package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BladeRuntimeError;

public abstract class NBitXorNode extends NBinaryNode {

  @Specialization
  protected long doLongs(long left, long right) {
    return left ^ right;
  }

  @Specialization(replaces = "doLongs")
  protected long doDoubles(double left, double right) {
    return (long)left ^ (long)right;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this,"^", left, right);
  }
}
