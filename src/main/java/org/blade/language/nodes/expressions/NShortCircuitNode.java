package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.blade.language.nodes.NNode;

public abstract class NShortCircuitNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode left;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode right;

  protected abstract boolean isEvaluateRight(boolean leftValue);

  protected abstract boolean execute(boolean leftValue, boolean rightValue);

  /**
   * Short circuits might be used just like a conditional statement it makes sense to profile the
   * branch probability.
   */
  private final CountingConditionProfile evaluateRightProfile = CountingConditionProfile.create();

  public NShortCircuitNode(NNode left, NNode right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public final Object execute(VirtualFrame frame) {
    return executeBoolean(frame);
  }

  @Override
  public final boolean executeBoolean(VirtualFrame frame) {
    boolean leftValue = left.executeBoolean(frame);

    boolean rightValue;
    if (evaluateRightProfile.profile(isEvaluateRight(leftValue))) {
      rightValue = right.executeBoolean(frame);
    } else {
      rightValue = false;
    }

    return execute(leftValue, rightValue);
  }
}
