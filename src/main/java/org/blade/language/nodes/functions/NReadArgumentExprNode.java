package org.blade.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;

public final class NReadArgumentExprNode extends NNode {
  @CompilerDirectives.CompilationFinal
  public final int index;

  @CompilerDirectives.CompilationFinal
  public final String name;

  public NReadArgumentExprNode(int index, String name) {
    this.index = index;
    this.name = name;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return frame.getArguments()[index];
  }
}
