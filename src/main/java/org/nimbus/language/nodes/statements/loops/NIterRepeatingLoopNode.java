package org.nimbus.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import org.nimbus.language.nodes.NNode;

public class NIterRepeatingLoopNode extends Node implements RepeatingNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode condition;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode iterator;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode body;

  public NIterRepeatingLoopNode(NNode condition, NNode iterator, NNode body) {
    this.condition = condition;
    this.iterator = iterator;
    this.body = body;
  }

  @Override
  public boolean executeRepeating(VirtualFrame frame) {
    if (condition != null && !condition.executeBoolean(frame)) {
      return false;
    }

    try {
      body.execute(frame);
    } catch (NBreakException e) {
      return false;
    } catch (NContinueException ignored) {
    }

    if (iterator != null) {
      iterator.execute(frame);
    }

    return true;
  }
}
