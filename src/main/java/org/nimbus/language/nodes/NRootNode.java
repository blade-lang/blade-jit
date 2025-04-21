package org.nimbus.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.nimbus.language.NimbusLanguage;

public class NRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode stmtNode;

  public NRootNode(NimbusLanguage language, NNode stmtNode) {
    this(language, FrameDescriptor.newBuilder().build(), stmtNode);
    this.stmtNode = stmtNode;
  }

  public NRootNode(NimbusLanguage language, FrameDescriptor frameDescriptor, NNode stmtNode) {
    super(language, frameDescriptor);
    this.stmtNode = stmtNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return stmtNode.execute(frame);
  }

  @Override
  public String toString() {
    return stmtNode.toString();
  }
}
