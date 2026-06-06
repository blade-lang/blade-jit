package org.zuri.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.zuri.language.nodes.NNode;

public final class NSelfLiteralNode extends NNode {
  @Override
  public Object execute(VirtualFrame frame) {
    return frame.getArguments()[0];
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return true;
  }
}
