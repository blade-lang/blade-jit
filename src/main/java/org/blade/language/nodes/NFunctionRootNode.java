package org.blade.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.functions.NFunctionBodyNode;

public final class NFunctionRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NStmtNode block;

  private final String name;
  private final MaterializedFrame parentFrame;

  public NFunctionRootNode(BladeLanguage language, FrameDescriptor frameDescriptor, NFunctionBodyNode block, String name, MaterializedFrame parentFrame) {
    super(language, frameDescriptor);
    this.block = block;
    this.name = name;
    this.parentFrame = parentFrame;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if (parentFrame != null) {
      frame.setObject(0, parentFrame);
    }

    return block.execute(frame);
  }

  @Override
  public String toString() {
    return "NRootFunctionNode";
  }

  @Override
  public String getName() {
    return name;
  }
}
