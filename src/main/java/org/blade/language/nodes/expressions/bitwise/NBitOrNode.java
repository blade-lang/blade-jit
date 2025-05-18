package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;

import java.math.BigInteger;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NBitOrNode extends NBinaryNode {

  @Specialization
  protected static long doLongs(long left, long right) {
    return left | right;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().or(BigInteger.valueOf(right)));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).or(right.get()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().or(right.get()));
  }

  @Specialization(replaces = {"doLongs"})
  protected static long doDoubles(double left, double right) {
    return (long)left | (long)right;
  }

  @Specialization
  protected static long doDoubleBigInt(double left, BigIntObject right) {
    return (long) left | bigToLong(right.get());
  }

  @Specialization
  protected static long doDoubleBigInt(BigIntObject left, double right) {
    return bigToLong(left.get()) | (long)right;
  }

  @Specialization(limit = "3")
  protected static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, "|", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  protected static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node,"|", left, right);
  }
}
