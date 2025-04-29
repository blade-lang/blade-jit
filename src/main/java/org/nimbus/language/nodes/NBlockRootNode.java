package org.nimbus.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.statements.NBlockStmtNode;

public final class NBlockRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NBlockStmtNode block;

  private final String name;

  public NBlockRootNode(NimbusLanguage language, NBlockStmtNode block, String name) {
    this(language, FrameDescriptor.newBuilder().build(), block, name);
  }

  public NBlockRootNode(NimbusLanguage language, FrameDescriptor frameDescriptor, NBlockStmtNode block, String name) {
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
    return block.getDescription();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SourceSection getSourceSection() {
    return block.getSourceSection();
  }
}
