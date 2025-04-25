package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NPowNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLongs(long left, long right) {
    double result = Math.pow(left, right);
    if(result < Long.MIN_VALUE || result > Long.MAX_VALUE) {
      throw new ArithmeticException();
    }

    return (long) result;
  }

  @Specialization(guards = {"isDouble(left)", "isLong(right)"})
  protected double doDoubleLong(double left, long right) {
    return Math.pow(left, (double) right);
  }

  @Specialization(guards = {"isLong(left)", "isDouble(right)"})
  protected double doLongDouble(long left, double right) {
    return Math.pow((double) left, right);
  }

  @Specialization(replaces = "doLongs")
  protected double doDoubles(double left, double right) {
    return Math.pow(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"**", left, right);
  }
}
