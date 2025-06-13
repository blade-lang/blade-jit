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

import java.math.BigInteger;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NLessThanNode extends NBinaryNode {

  @Specialization
  protected static boolean doLongs(long left, long right) {
    return left < right;
  }

  @Specialization
  protected static boolean doDoubles(double left, double right) {
    return left < right;
  }

  @Specialization
  protected static boolean doBigInts(BigIntObject left, BigIntObject right) {
    return compareBigInts(left.get(), right.get()) < 0;
  }

  @Specialization
  protected static boolean doStrings(TruffleString left, TruffleString right,
                                     @Cached TruffleString.CompareBytesNode compareNode) {
    return compareNode.execute(left, right, BladeLanguage.ENCODING) < 0;
  }

  @Specialization(limit = "3")
  protected static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, "<", left, right, interopLibrary);
    if (overrideValue != null) {
      return evaluateBoolean(overrideValue);
    }

    return doUnsupported(left, right);
  }

  @Fallback
  protected static boolean doUnsupported(Object left, Object right) {
    return false;
  }

  @CompilerDirectives.TruffleBoundary
  protected static int compareBigInts(BigInteger left, BigInteger right) {
    return left.compareTo(right);
  }
}
