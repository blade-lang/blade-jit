package org.nimbus.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import org.nimbus.language.nodes.NNode;

public final class NWhileRepeatingNode extends Node implements RepeatingNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode condition;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode body;

  public NWhileRepeatingNode(NNode condition, NNode body) {
    this.condition = condition;
    this.body = body;
  }

  @Override
  public boolean executeRepeating(VirtualFrame frame) {
    if (!condition.executeBoolean(frame)) {
      return false;
    }

    try {
      body.execute(frame);
    } catch (NBreakException e) {
      return false;
    } catch (NContinueException ignored) {
    }

    return true;
  }
}
