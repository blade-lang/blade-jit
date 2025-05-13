package org.blade.language.nodes.literals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;

public final class NIntLiteralNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final int value;

  public NIntLiteralNode(int value) {
    this.value = value;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return value;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return value > 0;
  }
}
