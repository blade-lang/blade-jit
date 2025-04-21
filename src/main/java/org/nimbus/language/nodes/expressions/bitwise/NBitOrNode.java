package org.nimbus.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NBitOrNode extends NBinaryNode {

  @Specialization
  protected long doLongs(long left, long right) {
    return left | right;
  }

  @Specialization(replaces = "doLongs")
  protected long doDoubles(double left, double right) {
    return (long)left | (long)right;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.create("operation | is undefined for object of types");
  }
}
