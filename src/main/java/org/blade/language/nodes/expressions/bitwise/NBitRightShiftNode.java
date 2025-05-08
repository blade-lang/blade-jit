package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BladeRuntimeError;

public abstract class NBitRightShiftNode extends NBinaryNode {

  @Specialization
  protected int doInts(int left, int right) {
    return (int)left >> (toUInt32(right) & 31);
  }

  @Specialization(replaces = "doInts")
  protected int doDoubles(double left, double right) {
    return (int)left >> (toUInt32(right) & 31);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this,">>", left, right);
  }

  private int toUInt32(int value) {
    return ((int) value & Integer.MAX_VALUE);
  }

  private int toUInt32(double value) {
    return ((int) value & Integer.MAX_VALUE);
  }
}
