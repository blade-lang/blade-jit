package org.nimbus.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NUnaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NBitNotNode extends NUnaryNode {

  @Specialization
  protected long doLongs(long value) {
    return ~(int)value;
  }

  @Specialization(replaces = "doLongs")
  protected long doDoubles(double value) {
    return ~(int)value;
  }

  @Fallback
  protected double doUnsupported(Object left) {
    throw NimRuntimeError.create("operation ~ is undefined for object type");
  }
}
