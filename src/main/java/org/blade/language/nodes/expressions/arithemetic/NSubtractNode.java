package org.blade.language.nodes.expressions.arithemetic;

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

public abstract class NSubtractNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLongs(long left, long right) {
    return Math.subtractExact(left, right);
  }

  @Specialization(guards = {"isDouble(left)", "isLong(right)"})
  protected double doDoubleLong(double left, long right) {
    return left - right;
  }

  @Specialization(guards = {"isLong(left)", "isDouble(right)"})
  protected double doLongDouble(long left, double right) {
    return (double)left - right;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().subtract(BigInteger.valueOf(right)));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).subtract(right.get()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().subtract(right.get()));
  }

  @Specialization(replaces = {"doLongs"})
  protected double doDoubles(double left, double right) {
    return left - right;
  }

  @Specialization
  protected double doDoubleBigInt(double left, BigIntObject right) {
    return left - bigToLong(right.get());
  }

  @Specialization
  protected double doDoubleBigInt(BigIntObject left, double right) {
    return bigToLong(left.get()) - right;
  }

  @Specialization(limit = "3")
  protected Object doObjects(BladeObject left, BladeObject right, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride("-", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this,"-", left, right);
  }
}
