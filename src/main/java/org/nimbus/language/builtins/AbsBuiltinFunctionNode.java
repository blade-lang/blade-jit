package org.nimbus.language.builtins;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.NBuiltinFunctionNode;

@GenerateNodeFactory
public abstract class AbsBuiltinFunctionNode extends NBuiltinFunctionNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLong(long arg) {
    return arg < 0 ? Math.negateExact(arg) : arg;
  }

  @Specialization(replaces = "doLong")
  protected double doDouble(double arg) {
    return Math.abs(arg);
  }

  @Fallback
  protected double notANumber(Object object) {
    return Double.NaN;
  }
}
