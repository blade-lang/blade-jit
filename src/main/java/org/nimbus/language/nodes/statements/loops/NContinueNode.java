package org.nimbus.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;

public final class NContinueNode extends NStmtNode {
  @Override
  public Object execute(VirtualFrame frame) {
    throw NContinueException.SINGLETON;
  }
}
