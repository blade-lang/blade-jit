package org.blade.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NStmtNode;

public final class NBreakNode extends NStmtNode {
  @Override
  public Object execute(VirtualFrame frame) {
    throw NBreakException.SINGLETON;
  }
}
