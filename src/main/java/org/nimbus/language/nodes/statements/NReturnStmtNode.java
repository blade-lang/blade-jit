package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;

public class NReturnStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode value;

  public NReturnStmtNode(NNode value) {
    this.value = value;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    throw new NReturnException(value.execute(frame));
  }
}
