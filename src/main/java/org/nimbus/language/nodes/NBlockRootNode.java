package org.nimbus.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.statements.NBlockStmtNode;

public final class NBlockRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NBlockStmtNode block;

  public NBlockRootNode(NimbusLanguage language, NBlockStmtNode block) {
    this(language, null, block);
  }

  public NBlockRootNode(NimbusLanguage language, FrameDescriptor frameDescriptor, NBlockStmtNode block) {
    super(language, frameDescriptor);
    this.block = block;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return block.execute(frame);
  }
}
