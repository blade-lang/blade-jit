package org.zuri.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.zuri.language.nodes.NStmtNode;

public final class NBreakNode extends NStmtNode {
  @Override
  public Object execute(VirtualFrame frame) {
    throw NBreakException.SINGLETON;
  }
}
