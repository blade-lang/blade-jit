package org.nimbus.language.translator;

import com.oracle.truffle.api.frame.FrameDescriptor;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.statements.NBlockStmtNode;

import java.util.List;

public class NTranslateResult {
  public final NBlockStmtNode node;
  public final FrameDescriptor frameDescriptor;

  public NTranslateResult(List<NNode> nodeList, FrameDescriptor frameDescriptor) {
    node = new NBlockStmtNode(nodeList);
    this.frameDescriptor = frameDescriptor;
  }
}
