package org.blade.language.nodes.expressions.logical;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeObject;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NEqualNode extends NBinaryNode {

  @Specialization
  public static boolean doBoolean(boolean left, boolean right) {
    return left == right;
  }

  @Specialization
  public static boolean doLongs(long left, long right) {
    return left == right;
  }

  @Specialization
  public static boolean doBigInts(BigIntObject left, BigIntObject right) {
    return left.equals(right);
  }

  @Specialization
  public static boolean doDoubles(double left, double right) {
    return left == right;
  }

  @Specialization
  public static boolean doStrings(String left, String right) {
    return left.equals(right);
  }

  @Specialization
  public static boolean doTruffleStrings(TruffleString left, TruffleString right,
                                            @Cached TruffleString.EqualNode equalNode) {
    return equalNode.execute(left, right, BladeLanguage.ENCODING);
  }

  @Specialization(limit = "3")
  public static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, "==", left, right, interopLibrary);
    if (overrideValue != null) {
      return evaluateBoolean(overrideValue);
    }

    return doUnsupported(left, right);
  }

  @Fallback
  public static boolean doUnsupported(Object left, Object right) {
    return left == right;
  }
}
