package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NFloorDivideNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected int doInts(int left, int right) {
    return Math.divideExact(left, right);
  }

  @Specialization(replaces = "doInts")
  protected double doDoubles(double left, double right) {
    return Math.floor(left / right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"//", left, right);
  }
}
