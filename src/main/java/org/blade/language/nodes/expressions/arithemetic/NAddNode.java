package org.blade.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.*;

import java.math.BigInteger;

@OperationProxy.Proxyable(allowUncached = true)
@ImportStatic(BladeContext.class)
public abstract class NAddNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  public static long doLongs(long left, long right) {
    return Math.addExact(left, right);
  }

  @Specialization(guards = {"isDouble(left)", "isLong(right)"})
  public static double doDoubleLong(double left, long right) {
    return left + right;
  }

  @Specialization(guards = {"isLong(left)", "isDouble(right)"})
  public static double doLongDouble(long left, double right) {
    return left + right;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doBigIntLong(BigIntObject left, long right) {
    return new BigIntObject(left.get().add(BigInteger.valueOf(right)));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doLongBigInt(long left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).add(right.get()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().add(right.get()));
  }

  @Specialization(replaces = {"doLongs"})
  public static double doDoubles(double left, double right) {
    return left + right;
  }

  @Specialization
  public static double doDoubleBigInt(double left, BigIntObject right) {
    return left + bigToLong(right.get());
  }

  @Specialization
  public static double doDoubleBigInt(BigIntObject left, double right) {
    return bigToLong(left.get()) + right;
  }

  @Specialization
  public static TruffleString doStrings(TruffleString left, TruffleString right,
                                           @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return BString.concat(concatNode, left, right);
  }

  @Specialization
  public static TruffleString doStringLong(TruffleString left, long right,
                                              @Cached @Cached.Shared("fromLongNode") TruffleString.FromLongNode fromLongNode,
                                              @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return BString.concat(concatNode, left, BString.fromLong(fromLongNode, right));
  }

  @Specialization
  public static TruffleString doLongString(long left, TruffleString right,
                                              @Cached @Cached.Shared("fromLongNode") TruffleString.FromLongNode fromLongNode,
                                              @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return BString.concat(concatNode, BString.fromLong(fromLongNode, left), right);
  }

  @CompilerDirectives.TruffleBoundary
  @Specialization(guards = "isString(left, right)")
  public static TruffleString doStringConverted(Object left, Object right,
                                                   @Cached TruffleString.FromJavaStringNode leftFromJavaNode,
                                                   @Cached TruffleString.FromJavaStringNode rightFromJavaNode,
                                                   @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return BString.concat(
      concatNode,
      BString.fromObject(leftFromJavaNode, left),
      BString.fromObject(rightFromJavaNode, right)
    );
  }

  @Specialization
  public static Object doLists(ListObject left, ListObject right, @Bind Node node,
                                  @Cached(value = "get(node)", uncached = "getContext(node)") BladeContext context,
                                  @Cached("context.objectsModel.listShape") Shape listShape,
                                  @Cached("context.objectsModel.listObject") BladeClass listClass) {
    Object[] leftItems = left.items;
    Object[] rightItems = right.items;
    int leftLength = leftItems.length;
    int rightLength = rightItems.length;

    Object[] items = new Object[leftLength + rightLength];
    System.arraycopy(leftItems, 0, items, 0, leftLength);
    System.arraycopy(rightItems, 0, items, leftLength, rightLength);

    return new ListObject(listShape, listClass, items);
  }

  @Specialization(limit = "3")
  public static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, "+", left, right, interopLibrary);
    if (overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  public static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node, "+", left, right);
  }

  public static boolean isString(Object left, Object right) {
    return left instanceof TruffleString || right instanceof TruffleString;
  }

  /*private static boolean isPrimitive(Object value) {
    return RTypesGen.isImplicitDouble(value) ||
      RTypesGen.isBoolean(value) ||
      value == RemNil.SINGLETON;
  }*/
}
