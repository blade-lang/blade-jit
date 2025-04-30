package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;
import org.nimbus.language.runtime.NimContext;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimUtils;

import java.io.PrintWriter;

public final class NEchoStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode object;

  public NEchoStmtNode(NNode object) {
    this.object = object;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object value = object.execute(frame);
    NimContext.get(this).println(value);
    return NimNil.SINGLETON;
  }
}
