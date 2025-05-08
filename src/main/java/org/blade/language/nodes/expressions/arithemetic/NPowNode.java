package org.blade.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BladeRuntimeError;

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
    return Math.pow(left, right);
  }

  @Specialization(guards = {"isInt(left)", "isDouble(right)"})
  protected double doIntDouble(int left, double right) {
    return Math.pow(left, right);
  }

  @Specialization(replaces = "doInts")
  protected double doDoubles(double left, double right) {
    return Math.pow(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this,"**", left, right);
  }
}
