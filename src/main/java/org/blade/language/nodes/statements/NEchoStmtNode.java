package org.blade.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BladeContext;
import org.blade.language.runtime.BladeNil;

public final class NEchoStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode object;

  public NEchoStmtNode(NNode object) {
    this.object = object;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object value = object.execute(frame);
    BladeContext.get(this).println(value);
    return BladeNil.SINGLETON;
  }
}
