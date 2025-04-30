package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import org.nimbus.language.nodes.NNode;

public final class NReadFunctionArgsExprNode extends NNode {
  @CompilerDirectives.CompilationFinal
  public final int index;

  @CompilerDirectives.CompilationFinal
  public final String name;

  public NReadFunctionArgsExprNode(int index, String name) {
    this.index = index;
    this.name = name;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return frame.getArguments()[index];
  }
}
