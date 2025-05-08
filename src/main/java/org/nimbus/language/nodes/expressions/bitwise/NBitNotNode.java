package org.nimbus.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NUnaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

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
    throw NimRuntimeError.argumentError(this,"~", left);
  }
}
