package org.blade.language.nodes.expressions.logical;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NUnaryNode;
import org.blade.language.runtime.ListObject;

public abstract class NLogicalNotNode extends NUnaryNode {
  @Specialization
  public boolean doBoolean(boolean value) {
    return !value;
  }

  @Specialization
  public boolean doInt(int value) {
    return value <= 0;
  }

  @Specialization
  public boolean doDouble(double value) {
    return value <= 0.0;
  }

  @Specialization
  public boolean doString(TruffleString value) {
    return value.isEmpty();
  }

  @Specialization
  public boolean doList(ListObject value) {
    return value.getArraySize() == 0;
  }

  @Fallback
  public boolean doOthers(Object value) {
    return !evaluateBoolean(value);
  }
}
