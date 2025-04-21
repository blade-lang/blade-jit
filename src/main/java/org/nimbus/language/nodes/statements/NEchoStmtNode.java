package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimUtils;

public final class NEchoStmtNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode object;

  public NEchoStmtNode(NNode object) {
    this.object = object;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    NimUtils.print(object.execute(frame));
    return NimNil.SINGLETON;
  }
}
