package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NPowNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected int doInts(int left, int right) {
    double result = Math.pow(left, right);
    if(result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) {
      throw new ArithmeticException();
    }

    return (int) result;
  }

  @Specialization(guards = {"isDouble(left)", "isInt(right)"})
  protected double doDoubleInt(double left, int right) {
    return Math.pow(left, (double) right);
  }

  @Specialization(guards = {"isInt(left)", "isDouble(right)"})
  protected double doIntDouble(int left, double right) {
    return Math.pow((double) left, right);
  }

  @Specialization(replaces = "doInts")
  protected double doDoubles(double left, double right) {
    return Math.pow(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"**", left, right);
  }
}
