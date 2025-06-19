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
  public static boolean doBoolean(boolean value) {
    return !value;
  }

  @Specialization
  public static boolean doLong(long value) {
    return value <= 0;
  }

  @Specialization
  public static boolean doDouble(double value) {
    return value <= 0.0;
  }

  @Specialization
  public static boolean doString(TruffleString value) {
    return value.isEmpty();
  }

  @Specialization
  public static boolean doList(ListObject value) {
    return value.getArraySize() == 0;
  }

  @Fallback
  public static boolean doOthers(Object value) {
    return !evaluateBoolean(value);
  }
}
