package org.blade.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeNil;

public final class NNilLiteralNode extends NNode {
  @Override
  public Object execute(VirtualFrame frame) {
    return BladeNil.SINGLETON;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return false;
  }
}
