package org.blade.language.nodes.expressions.arithemetic;

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
public abstract class NDivideNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected static long doLongs(long left, long right) {
    if (left % right == 0) {
      return left / right;
    }
    throw new ArithmeticException();
  }

  @Specialization(guards = {"isDouble(left)", "isLong(right)"})
  protected static double doDoubleLong(double left, long right) {
    return left / right;
  }

  @Specialization(guards = {"isLong(left)", "isDouble(right)"})
  protected static double doLongDouble(long left, double right) {
    return (double) left / right;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().divide(BigInteger.valueOf(right)));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).divide(right.get()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().divide(right.get()));
  }

  @Specialization(replaces = {"doLongs"})
  protected static double doDoubles(double left, double right) {
    return left / right;
  }

  @Specialization
  protected static double doDoubleBigInt(double left, BigIntObject right) {
    return left / bigToLong(right.get());
  }

  @Specialization
  protected static double doDoubleBigInt(BigIntObject left, double right) {
    return bigToLong(left.get()) / right;
  }

  @Specialization(limit = "3")
  protected static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, "/", left, right, interopLibrary);
    if (overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  protected static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node, "/", left, right);
  }

  protected static boolean isCornerCase(long a, long b) {
    return a != 0L && !(b == -1L && a == Long.MIN_VALUE);
  }
}
