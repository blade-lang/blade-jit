package org.blade.language.nodes.statements.loops;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BladeNil;

public final class NWhileStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private LoopNode loop;

  public NWhileStmtNode(NNode condition, NNode body) {
    loop = Truffle.getRuntime().createLoopNode(new NWhileRepeatingNode(condition, body));
  }

  @Override
  public Object execute(VirtualFrame frame) {
    loop.execute(frame);
    return BladeNil.SINGLETON;
  }
}
