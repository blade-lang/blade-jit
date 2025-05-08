package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NModuloNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class, guards = "right > 0")
  protected int doInts(int left, int right) {
    return Math.floorMod(left, right);
  }

  @Specialization(rewriteOn = ArithmeticException.class, guards = "left > 0")
  protected int doInts2(int left, int right) {
    return doInts(left, right);
  }

  @Specialization(rewriteOn = ArithmeticException.class, guards = "isCornerCase(left, right)")
  protected int doInts3(int left, int right) {
    return doInts(left, right);
  }

  @Specialization(guards = {"isDouble(left)", "isInt(right)"})
  protected double doDoubleInt(double left, int right) {
    return left % right;
  }

  @Specialization(guards = {"isInt(left)", "isDouble(right)"})
  protected double doIntDouble(int left, double right) {
    return (double)left % right;
  }

  @Specialization(replaces = {"doInts", "doInts2", "doInts3"})
  protected double doDoubles(double left, double right) {
    return left % right;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"%", left, right);
  }

  protected static boolean isCornerCase(int a, int b) {
    return a != 0L && !(b == -1L && a == Integer.MIN_VALUE);
  }
}
