package org.blade.language.nodes.literals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;

public final class NLongLiteralNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final long value;

  public NLongLiteralNode(long value) {
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
