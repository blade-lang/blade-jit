package org.blade.language.nodes.expressions.arithemetic;

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
public abstract class NPowNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected static long doLongs(long left, long right) {
    double result = Math.pow(left, right);
    if(result < Long.MIN_VALUE || result > Long.MAX_VALUE) {
      throw new ArithmeticException();
    }

    return (long) result;
  }

  @Specialization(guards = {"isDouble(left)", "isLong(right)"})
  protected static double doDoubleLong(double left, long right) {
    return Math.pow(left, (double) right);
  }

  @Specialization(guards = {"isLong(left)", "isDouble(right)"})
  protected static double doLongDouble(long left, double right) {
    return Math.pow((double) left, right);
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().pow((int)right));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).pow(right.get().intValue()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().pow(right.get().intValue()));
  }

  @Specialization(replaces = {"doLongs"})
  protected static double doDoubles(double left, double right) {
    return Math.pow(left, right);
  }

  @Specialization
  protected static double doDoubleBigInt(double left, BigIntObject right) {
    return Math.pow(left, bigToLong(right.get()));
  }

  @Specialization
  protected static double doDoubleBigInt(BigIntObject left, double right) {
    return Math.pow(bigToLong(left.get()), right);
  }

  @Specialization(limit = "3")
  protected static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, "**", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  protected static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node,"**", left, right);
  }
}
