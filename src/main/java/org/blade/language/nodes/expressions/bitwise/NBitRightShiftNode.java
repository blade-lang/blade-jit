package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;

import java.math.BigInteger;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NBitRightShiftNode extends NBinaryNode {

  @Specialization
  public static long doLongs(long left, long right) {
    return (int) left >> (toUInt32(right) & 31);
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().shiftRight((int) right));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).shiftRight(right.get().intValue()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().shiftRight(right.get().intValue()));
  }

  @Specialization(replaces = {"doLongs"})
  public static long doDoubles(double left, double right) {
    return (int) left >> (toUInt32(right) & 31);
  }

  @Specialization
  public static long doDoubleBigInt(double left, BigIntObject right) {
    return (long) left >> (toUInt32(bigToLong(right.get())) & 31);
  }

  @Specialization
  public static long doDoubleBigInt(BigIntObject left, double right) {
    return bigToLong(left.get()) >> (toUInt32((long) right) & 31);
  }

  @Specialization(limit = "3")
  public static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, ">>", left, right, interopLibrary);
    if (overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  public static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node, ">>", left, right);
  }

  private static int toUInt32(long value) {
    return ((int) value & Integer.MAX_VALUE);
  }

  private static int toUInt32(double value) {
    return ((int) value & Integer.MAX_VALUE);
  }
}
