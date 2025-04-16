package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NDivideNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected double doLongs(long left, long right) {
    return (double) left / right;
  }

  @Specialization(replaces = "doLongs")
  protected double doDoubles(double left, double right) {
    return left / right;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw new NimRuntimeError("operation / is undefined for object of types");
  }
}
