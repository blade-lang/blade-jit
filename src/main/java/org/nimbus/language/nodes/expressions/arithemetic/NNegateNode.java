package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NUnaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NNegateNode extends NUnaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLong(long value) {
    return -value;
  }

  @Specialization(replaces = "doLong")
  protected double doDouble(double value) {
    return -value;
  }

  @Fallback
  protected double doUnsupported(Object value) {
    throw NimRuntimeError.create("operation - is undefined for object type");
  }
}
