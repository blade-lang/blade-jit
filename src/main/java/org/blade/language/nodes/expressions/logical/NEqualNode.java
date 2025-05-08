package org.blade.language.nodes.expressions.logical;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NBinaryNode;

public abstract class NEqualNode extends NBinaryNode {

  @Specialization
  protected boolean doBoolean(boolean left, boolean right) {
    return left == right;
  }

  @Specialization
  protected boolean doLongs(long left, long right) {
    return left == right;
  }

  @Specialization(replaces = "doLongs")
  protected boolean doDoubles(double left, double right) {
    return left == right;
  }

  @Specialization
  protected boolean doStrings(TruffleString left, TruffleString right,
                           @Cached TruffleString.EqualNode equalNode) {
    return equalNode.execute(left, right, BladeLanguage.ENCODING);
  }

  @Fallback
  protected boolean doUnsupported(Object left, Object right) {
    return left == right;
  }
}
