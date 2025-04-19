package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.statements.NBlockStmtNode;
import org.nimbus.language.nodes.statements.NReturnException;
import org.nimbus.language.runtime.NimNil;

public final class NFunctionBodyNode extends NNode {
  @Children
  private final NNode[] nodes;

  private final BranchProfile exceptionTaken = BranchProfile.create();
  private final BranchProfile nullTaken = BranchProfile.create();

  public NFunctionBodyNode(NBlockStmtNode node) {
    this.nodes = node.nodes;
  }

  @Override
  @ExplodeLoop
  public Object execute(VirtualFrame frame) {
    for (NNode node : nodes) {
      try {
        if (node != null) {
          node.execute(frame);
        }
      } catch (NReturnException e) {
        exceptionTaken.enter();
        return e.value;
      }
    }

    nullTaken.enter();
    return NimNil.SINGLETON;
  }
}
