package org.zuri.language.translator;

import com.oracle.truffle.api.frame.FrameDescriptor;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.statements.NBlockStmtNode;

import java.util.List;

public class NTranslateResult {
  public final NBlockStmtNode node;
  public final FrameDescriptor frameDescriptor;

  public NTranslateResult(List<NNode> nodeList, FrameDescriptor frameDescriptor) {
    node = new NBlockStmtNode(nodeList);
    this.frameDescriptor = frameDescriptor;
  }
}
