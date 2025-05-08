package org.blade.language.nodes.statements.loops;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.blade.language.nodes.NNode;

public final class NWhileRepeatingNode extends Node implements RepeatingNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode condition;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode body;

  private final BranchProfile continueTaken = BranchProfile.create();
  private final BranchProfile breakTaken = BranchProfile.create();

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
      breakTaken.enter();
      return false;
    } catch (NContinueException ignored) {
      continueTaken.enter();
    }

    return true;
  }
}
