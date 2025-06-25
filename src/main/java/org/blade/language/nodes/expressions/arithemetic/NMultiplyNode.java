package org.blade.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.*;

import java.math.BigInteger;

@ImportStatic({Integer.class, BladeContext.class})
@OperationProxy.Proxyable(allowUncached = true)
public abstract class NMultiplyNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  public static long doLongs(long left, long right) {
    return Math.multiplyExact(left, right);
  }

  @Specialization(guards = {"isDouble(left)", "isLong(right)"})
  public static double doDoubleLong(double left, long right) {
    return left * (double) right;
  }

  @Specialization(guards = {"isLong(left)", "isDouble(right)"})
  public static double doLongDouble(long left, double right) {
    return (double) left * right;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().multiply(BigInteger.valueOf(right)));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).multiply(right.get()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().multiply(right.get()));
  }

  @Specialization(replaces = {"doLongs"})
  public static double doDoubles(double left, double right) {
    return left * right;
  }

  @Specialization
  public static double doDoubleBigInt(double left, BigIntObject right) {
    return left * bigToLong(right.get());
  }

  @Specialization
  public static double doDoubleBigInt(BigIntObject left, double right) {
    return bigToLong(left.get()) * right;
  }

  @Specialization
  public static TruffleString doStringMultiplication(TruffleString string, long count,
                                                        @Cached TruffleString.RepeatNode repeatNode) {
    return repeatNode.execute(string, (int) count, BladeLanguage.ENCODING);
  }

  @Specialization(guards = "count <= MAX_VALUE")
  public static ListObject doListMultiplication(ListObject list, long count, @Bind Node node,
                                                   @Cached(value = "get(node)", uncached = "getContext(node)") BladeContext context,
                                                   @Cached("context.objectsModel.listShape") Shape listShape,
                                                   @Cached("context.objectsModel.listObject") BladeClass listClass) {
    return new ListObject(
      listShape,
      listClass,
      repeatList(list, count)
    );
  }

  @Specialization(guards = "count > MAX_VALUE")
  public static ListObject doListMultiplicationOutOfBound(ListObject list, long count, @Bind Node node) {
    throw BladeRuntimeError.error(
      node,
      "List multiplication count out of bounds (",
      count,
      " > ",
      Integer.MAX_VALUE,
      ")"
    );
  }

  private static Object[] repeatList(ListObject list, long count) {
    int size = (int) list.getArraySize();
    int finalSize = (int) (size * count);

    Object[] objects = new Object[finalSize];

    for (int i = 0; i < count; i++) {
      System.arraycopy(list.items, 0, objects, i * size, size);
    }

    return objects;
  }

  @Specialization(limit = "3")
  public static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, "*", left, right, interopLibrary);
    if (overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  public static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node, "*", left, right);
  }

  public static BladeContext getContext(Node node) {
    return BladeContext.get(node);
  }
}
