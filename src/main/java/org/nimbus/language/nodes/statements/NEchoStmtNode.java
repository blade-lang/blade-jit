package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimNil;

public final class NEchoStmtNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode object;

  public NEchoStmtNode(NNode object) {
    this.object = object;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    System.out.println(object.execute(frame));
    return NimNil.SINGLETON;
  }
}
