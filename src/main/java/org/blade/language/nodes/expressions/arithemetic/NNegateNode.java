package org.blade.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.blade.language.nodes.NUnaryNode;
import org.blade.language.runtime.BladeRuntimeError;

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
    throw BladeRuntimeError.argumentError(this,"-", value);
  }
}
