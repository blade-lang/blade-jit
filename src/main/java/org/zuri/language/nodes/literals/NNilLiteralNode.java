package org.zuri.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.zuri.language.nodes.NNode;
import org.zuri.language.runtime.ZuriNil;

public final class NNilLiteralNode extends NNode {
  @Override
  public Object execute(VirtualFrame frame) {
    return ZuriNil.SINGLETON;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return false;
  }
}
