package org.blade.language.nodes.expressions.logical;

import org.blade.language.nodes.NNode;
import org.blade.language.nodes.expressions.NShortCircuitNode;

public final class NLogicalAndNode extends NShortCircuitNode {
  public NLogicalAndNode(NNode left, NNode right) {
    super(left, right);
  }

  @Override
  protected boolean isEvaluateRight(boolean left) {
    return left;
  }

  @Override
  protected boolean execute(boolean left, boolean right) {
    return left && right;
  }
}
