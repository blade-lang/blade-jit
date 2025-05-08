package org.blade.language.nodes.expressions.logical;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NBinaryNode;

public abstract class NGreaterThanOrEqualNode extends NBinaryNode {

  @Specialization
  protected boolean doLongs(long left, long right) {
    return left >= right;
  }

  @Specialization(replaces = "doLongs")
  protected boolean doDoubles(double left, double right) {
    return left >= right;
  }

  @Specialization
  protected boolean doStrings(TruffleString left, TruffleString right,
                              @Cached TruffleString.CompareBytesNode compareNode) {
    return compareNode.execute(left, right, BladeLanguage.ENCODING) >= 0;
  }

  @Fallback
  protected boolean doUnsupported(Object left, Object right) {
    return false;
  }
}
