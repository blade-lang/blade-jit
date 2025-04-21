package org.nimbus.language.nodes.statements.loops;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimNil;

public final class NIterStmtNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode initializer;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private LoopNode loop;

  public NIterStmtNode(NNode initializer, NNode condition, NNode iterator, NNode body) {
    this.initializer = initializer;
    loop = Truffle.getRuntime().createLoopNode(new NIterRepeatingLoopNode(
      condition,
      iterator,
      body
    ));
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if(initializer != null)
      initializer.execute(frame);
    loop.execute(frame);
    return NimNil.SINGLETON;
  }
}
