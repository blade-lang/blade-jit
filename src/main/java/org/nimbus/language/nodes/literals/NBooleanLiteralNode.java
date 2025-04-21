package org.nimbus.language.nodes.literals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;

public class NBooleanLiteralNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final boolean value;

  public NBooleanLiteralNode(boolean value) {
    this.value = value;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return value;
  }
}
