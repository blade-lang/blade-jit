package org.blade.language.nodes.expressions.logical;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;

public abstract class NEqualNode extends NBinaryNode {

  @Specialization
  protected boolean doBoolean(boolean left, boolean right) {
    return left == right;
  }

  @Specialization
  protected boolean doLongs(long left, long right) {
    return left == right;
  }

  @CompilerDirectives.TruffleBoundary
  @Specialization
  protected boolean doBigInts(BigIntObject left, BigIntObject right) {
    return left.equals(right);
  }

  @Specialization
  protected boolean doDoubles(double left, double right) {
    return left == right;
  }

  @Specialization
  protected boolean doStrings(String left, String right) {
    return left.equals(right);
  }

  @Specialization
  protected boolean doTruffleStrings(TruffleString left, TruffleString right,
                           @Cached TruffleString.EqualNode equalNode) {
    return equalNode.execute(left, right, BladeLanguage.ENCODING);
  }

  @Specialization(limit = "3")
  protected Object doObjects(BladeObject left, BladeObject right, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride("==", left, right, interopLibrary);
    if(overrideValue != null) {
      return evaluateBoolean(overrideValue);
    }

    return doUnsupported(left, right);
  }

  @Fallback
  protected boolean doUnsupported(Object left, Object right) {
    return left == right;
  }
}
