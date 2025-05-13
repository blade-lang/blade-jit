package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;

import java.math.BigInteger;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

public abstract class NBitRightShiftNode extends NBinaryNode {

  @Specialization
  protected int doInts(int left, int right) {
    return (int)left >> (toUInt32(right) & 31);
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().shiftRight((int)right));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).shiftRight(right.get().intValue()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().shiftRight(right.get().intValue()));
  }

  @Specialization(replaces = {"doInts"})
  protected int doDoubles(double left, double right) {
    return (int)left >> (toUInt32(right) & 31);
  }

  @Specialization
  protected long doDoubleBigInt(double left, BigIntObject right) {
    return (long) left >> (toUInt32(bigToLong(right.get())) & 31);
  }

  @Specialization
  protected long doDoubleBigInt(BigIntObject left, double right) {
    return bigToLong(left.get()) >> (toUInt32((long)right) & 31);
  }

  @Specialization(limit = "3")
  protected Object doObjects(BladeObject left, BladeObject right, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(">>", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this,">>", left, right);
  }

  private int toUInt32(int value) {
    return ((int) value & Integer.MAX_VALUE);
  }

  private int toUInt32(double value) {
    return ((int) value & Integer.MAX_VALUE);
  }
}
