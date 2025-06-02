package org.blade.language.nodes.statements;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;

@NodeChild(value = "value", type = NNode.class)
@NodeField(name = "slotName", type = String.class)
@NodeField(name = "slot", type = int.class)
public abstract class NLocalAssignNode extends NStmtNode {
  public abstract String getSlotName();
  public abstract int getSlot();

  @Specialization(guards = "isBooleanOrIllegal(frame)")
  protected boolean doBoolean(VirtualFrame frame, boolean value) {
    final int frameSlot = getSlot();
    frame.getFrameDescriptor().setSlotKind(frameSlot, FrameSlotKind.Boolean);
    frame.setBoolean(frameSlot, value);
    return value;
  }

  @Specialization(guards = "isLongOrIllegal(frame)")
  protected long doLong(VirtualFrame frame, long value) {
    final int frameSlot = getSlot();
    frame.getFrameDescriptor().setSlotKind(frameSlot, FrameSlotKind.Long);
    frame.setLong(frameSlot, value);
    return value;
  }

  @Specialization(replaces = "doLong", guards = "isDoubleOrIllegal(frame)")
  protected double doDouble(VirtualFrame frame, double value) {
    final int frameSlot = getSlot();
    frame.getFrameDescriptor().setSlotKind(frameSlot, FrameSlotKind.Double);
    frame.setDouble(frameSlot, value);
    return value;
  }

  @Specialization(replaces = {"doLong", "doDouble", "doBoolean"})
  protected Object doObject(VirtualFrame frame, Object value) {
    final int frameSlot = getSlot();
    frame.getFrameDescriptor().setSlotKind(frameSlot, FrameSlotKind.Object);
    frame.setObject(frameSlot, value);
    return value;
  }

  protected boolean isBooleanOrIllegal(VirtualFrame frame) {
    final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlot());
    return kind == FrameSlotKind.Boolean || kind == FrameSlotKind.Illegal;
  }

  protected boolean isLongOrIllegal(VirtualFrame frame) {
    final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlot());
    return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
  }

  protected boolean isDoubleOrIllegal(VirtualFrame frame) {
    final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlot());
    return kind == FrameSlotKind.Double || kind == FrameSlotKind.Illegal;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.WriteVariableTag.class || super.hasTag(tag);
  }
}
