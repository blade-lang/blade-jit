package org.zuri.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.NStmtNode;
import org.zuri.language.runtime.ZuriContext;
import org.zuri.language.runtime.ZuriNil;

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
    ZuriContext.get(this).println(value);
    return ZuriNil.SINGLETON;
  }
}
