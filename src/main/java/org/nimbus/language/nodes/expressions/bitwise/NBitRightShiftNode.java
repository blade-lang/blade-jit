package org.nimbus.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NBitRightShiftNode extends NBinaryNode {

  @Specialization
  protected long doLongs(long left, long right) {
    return (int)left >> (toUInt32(right) & 31);
  }

  @Specialization(replaces = "doLongs")
  protected long doDoubles(double left, double right) {
    return (int)left >> (toUInt32(right) & 31);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.create("operation >> is undefined for object of types");
  }

  private int toUInt32(long value) {
    return ((int) value & Integer.MAX_VALUE);
  }

  private int toUInt32(double value) {
    return ((int) value & Integer.MAX_VALUE);
  }
}
