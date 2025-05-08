package org.blade.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BladeRuntimeError;

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
    throw BladeRuntimeError.argumentError(this,"//", left, right);
  }
}
