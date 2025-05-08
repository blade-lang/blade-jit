package org.nimbus.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NBitLeftShiftNode extends NBinaryNode {

  @Specialization
  protected int doInts(int left, int right) {
    return toUInt32(left) << (toUInt32(right) & 31);
  }

  @Specialization(replaces = "doInts")
  protected int doDoubles(double left, double right) {
    return toUInt32(left) << (toUInt32(right) & 31);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"<<", left, right);
  }

  private int toUInt32(int value) {
    return value + Integer.MIN_VALUE;
  }

  private int toUInt32(double value) {
    return ((int) value + Integer.MIN_VALUE);
  }
}
