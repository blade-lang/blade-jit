package org.blade.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.statements.NBlockStmtNode;

public final class NBlockRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NBlockStmtNode block;

  private final String name;

  private final SourceSection sourceSection;

  public NBlockRootNode(BladeLanguage language, NBlockStmtNode block, String name) {
    this(language, FrameDescriptor.newBuilder().build(), block, name);
  }

  public NBlockRootNode(BladeLanguage language, FrameDescriptor frameDescriptor, NBlockStmtNode block, String name) {
    this(language, frameDescriptor, block, name, null);
  }

  public NBlockRootNode(BladeLanguage language, FrameDescriptor frameDescriptor, NBlockStmtNode block, String name, SourceSection sourceSection) {
    super(language, frameDescriptor);
    this.block = block;
    this.name = name;
    this.sourceSection = sourceSection;
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
    return sourceSection == null ? block.getSourceSection() : sourceSection;
  }
}
