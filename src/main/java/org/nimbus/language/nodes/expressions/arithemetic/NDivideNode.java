package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NDivideNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class, guards = "right > 0")
  protected long doLongs(long left, long right) {
    if(left % right == 0) {
      return left / right;
    }
    throw new ArithmeticException();
  }

  @Specialization(rewriteOn = ArithmeticException.class, guards = "left > 0")
  protected long doLongs2(long left, long right) {
    return doLongs(left, right);
  }

  @Specialization(rewriteOn = ArithmeticException.class, guards = "isCornerCase(left, right)")
  protected long doLongs3(long left, long right) {
    return doLongs(left, right);
  }

  @Specialization(replaces = {"doLongs", "doLongs2", "doLongs3"})
  protected double doDoubles(double left, double right) {
    return left / right;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.create("operation / is undefined for object of types");
  }

  protected static boolean isCornerCase(long a, long b) {
    return a != 0L && !(b == -1L && a == Long.MIN_VALUE);
  }
}
