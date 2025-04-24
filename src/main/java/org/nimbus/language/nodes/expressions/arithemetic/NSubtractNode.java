package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NSubtractNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLongs(long left, long right) {
    return Math.subtractExact(left, right);
  }

  @Specialization(replaces = "doLongs")
  protected double doDoubles(double left, double right) {
    return left - right;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"-", left, right);
  }
}
