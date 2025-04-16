package org.nimbus.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;

public final class NContinueNode extends NNode {
  @Override
  public Object execute(VirtualFrame frame) {
    throw new NContinueException();
  }
}
