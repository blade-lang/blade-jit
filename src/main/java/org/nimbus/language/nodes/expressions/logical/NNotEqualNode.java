package org.nimbus.language.nodes.expressions.logical;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NBinaryNode;

public abstract class NNotEqualNode extends NBinaryNode {

  @Specialization
  protected boolean doBoolean(boolean left, boolean right) {
    return left != right;
  }

  @Specialization
  protected boolean doInts(int left, int right) {
    return left != right;
  }

  @Specialization(replaces = "doInts")
  protected boolean doDoubles(double left, double right) {
    return left != right;
  }

  @Specialization
  protected boolean doStrings(TruffleString left, TruffleString right,
                           @Cached TruffleString.EqualNode equalNode) {
    return !equalNode.execute(left, right, NimbusLanguage.ENCODING);
  }

  @Fallback
  protected boolean doUnsupported(Object left, Object right) {
    return left != right;
  }
}
