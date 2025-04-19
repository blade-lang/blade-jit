package org.nimbus.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;

public final class NSelfLiteralNode extends NNode {
  @Override
  public Object execute(VirtualFrame frame) {
    return frame.getArguments()[0];
  }
}
