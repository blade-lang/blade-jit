package org.blade.language.nodes.statements;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;

@NodeChild(value = "value", type = NNode.class)
@NodeField(name = "slotName", type = String.class)
@NodeField(name = "slot", type = int.class)
@NodeField(name = "depth", type = int.class)
public abstract class NClosedAssignNode extends NStmtNode {
  public abstract String getSlotName();
  public abstract int getSlot();
  public abstract int getDepth();

  @Specialization(guards = "isBooleanOrIllegal(parentFrame)")
  protected boolean doBoolean(VirtualFrame frame, boolean value,
                              @Cached(value = "getParentFrame(frame, getDepth())", neverDefault = true) MaterializedFrame parentFrame) {
    final int frameSlot = getSlot();
    parentFrame.getFrameDescriptor().setSlotKind(frameSlot, FrameSlotKind.Boolean);
    parentFrame.setBoolean(frameSlot, value);
    return value;
  }

  @Specialization(guards = "isLongOrIllegal(parentFrame)")
  protected long doLong(VirtualFrame frame, long value,
                        @Cached(value = "getParentFrame(frame, getDepth())", neverDefault = true) MaterializedFrame parentFrame) {
    final int frameSlot = getSlot();
    parentFrame.getFrameDescriptor().setSlotKind(frameSlot, FrameSlotKind.Long);
    parentFrame.setLong(frameSlot, value);
    return value;
  }

  @Specialization(replaces = "doLong", guards = "isDoubleOrIllegal(parentFrame)")
  protected double doDouble(VirtualFrame frame, double value,
                            @Cached(value = "getParentFrame(frame, getDepth())", neverDefault = true) MaterializedFrame parentFrame) {
    final int frameSlot = getSlot();
    parentFrame.getFrameDescriptor().setSlotKind(frameSlot, FrameSlotKind.Double);
    parentFrame.setDouble(frameSlot, value);
    return value;
  }

  @Specialization(replaces = {"doLong", "doDouble", "doBoolean"})
  protected Object doObject(VirtualFrame frame, Object value,
                            @Cached(value = "getParentFrame(frame, getDepth())", neverDefault = true) MaterializedFrame parentFrame) {
    final int frameSlot = getSlot();
    parentFrame.getFrameDescriptor().setSlotKind(frameSlot, FrameSlotKind.Object);
    parentFrame.setObject(frameSlot, value);
    return value;
  }

  @Idempotent
  protected boolean isBooleanOrIllegal(VirtualFrame frame) {
    final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlot());
    return kind == FrameSlotKind.Boolean || kind == FrameSlotKind.Illegal;
  }

  @Idempotent
  protected boolean isLongOrIllegal(VirtualFrame frame) {
    final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlot());
    return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
  }

  @Idempotent
  protected boolean isDoubleOrIllegal(VirtualFrame frame) {
    final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlot());
    return kind == FrameSlotKind.Double || kind == FrameSlotKind.Illegal;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.WriteVariableTag.class || super.hasTag(tag);
  }
}
