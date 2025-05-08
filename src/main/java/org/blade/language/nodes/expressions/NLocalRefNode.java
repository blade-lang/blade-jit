package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;

@NodeField(name = "slot", type = int.class)
public abstract class NLocalRefNode extends NNode {
  protected abstract int getSlot();

  @Specialization(guards = "frame.isBoolean(getSlot())")
  protected boolean doBoolean(VirtualFrame frame) {
    return frame.getBoolean(getSlot());
  }

  @Specialization(guards = "frame.isInt(getSlot())")
  protected int doInt(VirtualFrame frame) {
    return frame.getInt(getSlot());
  }

  @Specialization(guards = "frame.isDouble(getSlot())", replaces = "doInt")
  protected double doDouble(VirtualFrame frame) {
    return frame.getDouble(getSlot());
  }

  @Specialization(replaces = {"doBoolean", "doInt", "doDouble"})
  protected Object doObject(VirtualFrame frame) {
    return frame.getObject(getSlot());
  }
}
