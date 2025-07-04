package org.blade.language.nodes.common;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.blade.language.nodes.NNode;

public final class NDynamicObjectRefNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final DynamicObject object;

  public NDynamicObjectRefNode(DynamicObject object) {
    this.object = object;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return object;
  }
}
