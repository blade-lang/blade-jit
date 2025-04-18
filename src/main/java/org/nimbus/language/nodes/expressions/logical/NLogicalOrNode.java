package org.nimbus.language.nodes.expressions.logical;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.expressions.NShortCircuitNode;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NString;

public final class NLogicalOrNode extends NShortCircuitNode {
  public NLogicalOrNode(NNode left, NNode right) {
    super(left, right);
  }

  @Override
  protected boolean isEvaluateRight(boolean left) {
    return !left;
  }

  @Override
  protected boolean execute(boolean left, boolean right) {
    return left || right;
  }
}
