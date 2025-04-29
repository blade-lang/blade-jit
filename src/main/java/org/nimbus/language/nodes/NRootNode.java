package org.nimbus.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.nimbus.language.NimbusLanguage;

public class NRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode stmtNode;

  private final String name;

  private final SourceSection sourceSection;

  public NRootNode(NimbusLanguage language, NNode stmtNode, String name) {
    this(language, FrameDescriptor.newBuilder().build(), stmtNode, name, stmtNode.getSourceSection());
  }

  public NRootNode(NimbusLanguage language, FrameDescriptor frameDescriptor, NNode stmtNode, String name, SourceSection sourceSection) {
    super(language, frameDescriptor);
    this.stmtNode = stmtNode;
    this.name = name;
    this.sourceSection = sourceSection;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return stmtNode.execute(frame);
  }

  @Override
  public String toString() {
    return stmtNode.toString();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SourceSection getSourceSection() {
    return sourceSection;
  }
}
