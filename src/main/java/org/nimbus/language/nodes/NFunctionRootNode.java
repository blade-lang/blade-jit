package org.nimbus.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.statements.NFunctionBodyNode;

public final class NFunctionRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NFunctionBodyNode block;

  public NFunctionRootNode(NimbusLanguage language, NFunctionBodyNode block) {
    this(language, null, block);
  }

  public NFunctionRootNode(NimbusLanguage language, FrameDescriptor frameDescriptor, NFunctionBodyNode block) {
    super(language, frameDescriptor);
    this.block = block;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return block.execute(frame);
  }
}
