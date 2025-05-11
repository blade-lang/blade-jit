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

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

public abstract class NBitLeftShiftNode extends NBinaryNode {

  @Specialization
  protected long doLongs(long left, long right) {
    return (int)(toUInt32(left) << (toUInt32(right) & 31));
  }

  @Specialization(replaces = "doLongs")
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().shiftLeft(right.get().intValue()));
  }

  @Specialization(replaces = "doBigInts", guards = {"leftLibrary.fitsInBigInteger(left)", "rightLibrary.fitsInBigInteger(right)"}, limit = "3")
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doInteropBigInteger(Object left, Object right,
                                                 @CachedLibrary("left") InteropLibrary leftLibrary,
                                                 @CachedLibrary("right") InteropLibrary rightLibrary) {
    try {
      return new BigIntObject(leftLibrary.asBigInteger(left).shiftLeft(rightLibrary.asBigInteger(right).intValue()));
    } catch (UnsupportedMessageException e) {
      throw shouldNotReachHere(e);
    }
  }

  @Specialization(replaces = "doInteropBigInteger")
  protected long doDoubles(double left, double right) {
    return (int)(toUInt32(left) << (toUInt32(right) & 31));
  }

  @Specialization(limit = "3")
  protected Object doObjects(BladeObject left, BladeObject right, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride("<<", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this,"<<", left, right);
  }

  private int toUInt32(long value) {
    return ((int) value + Integer.MIN_VALUE);
  }

  private int toUInt32(double value) {
    return ((int) value + Integer.MIN_VALUE);
  }
}
