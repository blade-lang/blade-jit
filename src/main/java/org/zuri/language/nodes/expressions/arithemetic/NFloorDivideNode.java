package org.zuri.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.zuri.language.nodes.NBinaryNode;
import org.zuri.language.runtime.BigIntObject;
import org.zuri.language.runtime.ZuriObject;
import org.zuri.language.runtime.ZuriRuntimeError;

import java.math.BigInteger;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NFloorDivideNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected static long doLongs(long left, long right) {
    return Math.divideExact(left, right);
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
    return Math.floor(left / right);
  }

  @Specialization
  protected static double doDoubleBigInt(double left, BigIntObject right) {
    return Math.floor(left / bigToLong(right.get()));
  }

  @Specialization
  protected static double doDoubleBigInt(BigIntObject left, double right) {
    return Math.floor(bigToLong(left.get()) / right);
  }

  @Specialization(limit = "3")
  protected static Object doObjects(ZuriObject left, ZuriObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, "//", left, right, interopLibrary);
    if (overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  protected static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw ZuriRuntimeError.argumentError(node, "//", left, right);
  }
}
