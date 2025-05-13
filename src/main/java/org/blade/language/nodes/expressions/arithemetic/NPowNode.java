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

public abstract class NPowNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected int doInts(int left, int right) {
    double result = Math.pow(left, right);
    if(result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) {
      throw new ArithmeticException();
    }

    return (int) result;
  }

  @Specialization(guards = {"isDouble(left)", "isInt(right)"})
  protected double doDoubleInt(double left, int right) {
    return Math.pow(left, right);
  }

  @Specialization(guards = {"isInt(left)", "isDouble(right)"})
  protected double doIntDouble(int left, double right) {
    return Math.pow(left, right);
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().pow((int)right));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).pow(right.get().intValue()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().pow(right.get().intValue()));
  }

  @Specialization(replaces = {"doInts"})
  protected double doDoubles(double left, double right) {
    return Math.pow(left, right);
  }

  @Specialization
  protected double doDoubleBigInt(double left, BigIntObject right) {
    return Math.pow(left, bigToLong(right.get()));
  }

  @Specialization
  protected double doDoubleBigInt(BigIntObject left, double right) {
    return Math.pow(bigToLong(left.get()), right);
  }

  @Specialization(limit = "3")
  protected Object doObjects(BladeObject left, BladeObject right, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride("**", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this,"**", left, right);
  }
}
