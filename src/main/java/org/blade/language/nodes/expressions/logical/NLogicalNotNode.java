package org.blade.language.nodes.expressions.logical;

import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NUnaryNode;
import org.blade.language.runtime.ListObject;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NLogicalNotNode extends NUnaryNode {

  @Specialization
  protected static boolean doBoolean(boolean value) {
    return !value;
  }

  @Specialization
  protected static boolean doLong(long value) {
    return value <= 0;
  }

  @Specialization
  protected static boolean doDouble(double value) {
    return value <= 0.0;
  }

  @Specialization
  protected static boolean doString(TruffleString value) {
    return value.isEmpty();
  }

  @Specialization
  protected static boolean doList(ListObject value) {
    return value.getArraySize() == 0;
  }

  @Fallback
  protected static boolean doOthers(Object value) {
    return !evaluateBoolean(value);
  }
}
