package org.nimbus.language.nodes.expressions.logical;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NUnaryNode;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NLogicalNotNode extends NUnaryNode {
  @Specialization
  public boolean doBoolean(boolean value) {
    return !value;
  }

  @Specialization
  public boolean doLong(long value) {
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
  public boolean doList(NListObject value) {
    return value.getArraySize() == 0;
  }

  @Fallback
  public boolean doOthers(Object value) {
    return !evaluateBoolean(value);
  }
}
