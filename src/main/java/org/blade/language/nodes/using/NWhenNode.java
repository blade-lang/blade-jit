package org.blade.language.nodes.using;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;

public final class NWhenNode extends NNode {
  @Child
  public NNode caseValueNode;

  @Child
  public NNode bodyNode;

  public NWhenNode(NNode caseValueNode, NNode bodyNode) {
    this.caseValueNode = caseValueNode;
    this.bodyNode = bodyNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return caseValueNode.execute(frame);
  }
}
