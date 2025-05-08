package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.blade.language.nodes.NUnaryNode;
import org.blade.language.runtime.BladeRuntimeError;

public abstract class NBitNotNode extends NUnaryNode {

  @Specialization
  protected int doInts(int value) {
    return ~(int)value;
  }

  @Specialization(replaces = "doInts")
  protected int doDoubles(double value) {
    return ~(int)value;
  }

  @Fallback
  protected double doUnsupported(Object left) {
    throw BladeRuntimeError.argumentError(this,"~", left);
  }
}
