package org.nimbus.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import org.nimbus.language.nodes.NNode;

public final class NDoWhileRepeatingNode extends Node implements RepeatingNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode condition;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode body;

  public NDoWhileRepeatingNode(NNode condition, NNode body) {
    this.condition = condition;
    this.body = body;
  }

  @Override
  public boolean executeRepeating(VirtualFrame frame) {
    try {
      body.execute(frame);
    } catch (NBreakException e) {
      return false;
    } catch (NContinueException ignored) {
    }

    return condition.executeBoolean(frame);
  }
}
