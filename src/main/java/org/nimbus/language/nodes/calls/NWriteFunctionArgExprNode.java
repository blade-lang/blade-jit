package org.nimbus.language.nodes.calls;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;

public final class NWriteFunctionArgExprNode extends NNode {
  @Child
  NNode value;

  @CompilerDirectives.CompilationFinal
  private final int index;

  public NWriteFunctionArgExprNode(NNode value, int index) {
    this.value = value;
    this.index = index;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object result = value.execute(frame);
    frame.getArguments()[index] = result;
    return result;
  }
}
