package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;

public final class NReadFunctionArgsExprNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final int index;

  public NReadFunctionArgsExprNode(int index) {
    this.index = index;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return frame.getArguments()[index];
  }
}
