package org.blade.language.nodes.using;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;

public final class NUsingNode extends NStmtNode {
  @SuppressWarnings({"FieldMayBeFinal", "unused"})
  @Child private NNode valueNode; // intentional: Fields must be adopted by a root node before they can be executed.

  private final NUsingDispatchNode dispatchNode;

  public NUsingNode(NNode valueNode, NWhenNode[] caseNodes, NNode defaultNode) {
    this.valueNode = valueNode;
    dispatchNode = NUsingDispatchNodeGen.create(caseNodes, defaultNode, valueNode);
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return dispatchNode.execute(frame);
  }
}
