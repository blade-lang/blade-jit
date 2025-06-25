package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;

public final class NClosedRefNode extends NNode {
  private final int slot;
  private final int depth;
  private MaterializedFrame parentFrame;

  public NClosedRefNode(int slot, int depth) {
    this.slot = slot;
    this.depth = depth;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if (parentFrame == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      parentFrame = getParentFrame(frame, depth);
    }

    FrameSlotKind kind = parentFrame.getFrameDescriptor().getSlotKind(slot);

    return switch (kind) {
      case Boolean -> parentFrame.getBoolean(slot);
      case Int -> parentFrame.getInt(slot);
      case Long -> parentFrame.getLong(slot);
      case Double -> parentFrame.getDouble(slot);
      default -> parentFrame.getObject(slot);
    };
  }
}
