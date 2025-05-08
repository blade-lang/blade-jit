package org.blade.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;

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
