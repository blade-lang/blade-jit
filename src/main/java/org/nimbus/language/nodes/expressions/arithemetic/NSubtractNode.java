package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NSubtractNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected int doInts(int left, int right) {
    return Math.subtractExact(left, right);
  }

  @Specialization(guards = {"isDouble(left)", "isInt(right)"})
  protected double doDoubleInt(double left, int right) {
    return left - right;
  }

  @Specialization(guards = {"isInt(left)", "isDouble(right)"})
  protected double doIntDouble(int left, double right) {
    return (double)left - right;
  }

  @Specialization(replaces = "doInts")
  protected double doDoubles(double left, double right) {
    return left - right;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"-", left, right);
  }
}
