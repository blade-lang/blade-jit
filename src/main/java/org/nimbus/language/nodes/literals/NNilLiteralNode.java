package org.nimbus.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimNil;

public final class NNilLiteralNode extends NNode {
  @Override
  public Object execute(VirtualFrame frame) {
    return NimNil.SINGLETON;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return false;
  }
}
