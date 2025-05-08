package org.blade.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.functions.NFunctionBodyNode;

public final class NFunctionRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NFunctionBodyNode block;

  private final String name;

  public NFunctionRootNode(BladeLanguage language, FrameDescriptor frameDescriptor, NFunctionBodyNode block, String name) {
    super(language, frameDescriptor);
    this.block = block;
    this.name = name;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return block.execute(frame);
  }

  @Override
  public String toString() {
    return "NFunctionRootNode";
  }

  @Override
  public String getName() {
    return name;
  }
}
