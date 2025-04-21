package org.nimbus.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.nimbus.language.nodes.NNode;

public final class NIterRepeatingLoopNode extends Node implements RepeatingNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode condition;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode iterator;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode body;

  private final BranchProfile continueTaken = BranchProfile.create();
  private final BranchProfile breakTaken = BranchProfile.create();

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
      breakTaken.enter();
      return false;
    } catch (NContinueException ignored) {
      continueTaken.enter();
    }

    if (iterator != null) {
      iterator.execute(frame);
    }

    return true;
  }

  @Override
  public String toString() {
    return "NIterRepeatingLoop";
  }
}
