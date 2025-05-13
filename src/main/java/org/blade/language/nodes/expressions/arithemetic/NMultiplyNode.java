package org.blade.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.*;
import org.blade.language.shared.BuiltinClassesModel;

import java.math.BigInteger;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@ImportStatic(Integer.class)
public abstract class NMultiplyNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doInts(int left, int right) {
    return Math.multiplyExact(left, right);
  }

  @Specialization(guards = {"isDouble(left)", "isInt(right)"})
  protected double doDoubleInt(double left, int right) {
    return left * (double) right;
  }

  @Specialization(guards = {"isInt(left)", "isDouble(right)"})
  protected double doIntDouble(int left, double right) {
    return (double) left * right;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigIntInt(BigIntObject left, int right) {
    return new BigIntObject(left.get().multiply(BigInteger.valueOf(right)));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doIntBigInt(int left, BigIntObject right) {
    return new BigIntObject(BigInteger.valueOf(left).multiply(right.get()));
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().multiply(right.get()));
  }

  @Specialization(replaces = {"doInts"})
  protected double doDoubles(double left, double right) {
    return left * right;
  }

  @Specialization
  protected double doDoubleBigInt(double left, BigIntObject right) {
    return left * bigToLong(right.get());
  }

  @Specialization
  protected double doDoubleBigInt(BigIntObject left, double right) {
    return bigToLong(left.get()) * right;
  }

  @Specialization
  protected TruffleString doStringMultiplication(TruffleString string, int count,
                                                 @Cached TruffleString.RepeatNode repeatNode) {
    return repeatNode.execute(string, count, BladeLanguage.ENCODING);
  }

  @Specialization(guards = "count <= MAX_VALUE")
  protected ListObject doListMultiplication(ListObject list, int count) {
    BuiltinClassesModel objectModel = BladeContext.get(this).objectsModel;
    return new ListObject(
      objectModel.listShape,
      objectModel.listObject,
      repeatList(list, count)
    );
  }

  @Specialization(guards = "count > MAX_VALUE")
  protected ListObject doListMultiplicationOutOfBound(ListObject list, int count) {
    throw BladeRuntimeError.create("List multiplication count out of bounds (", count, " > ", Integer.MAX_VALUE, ")");
  }

  @ExplodeLoop
  private Object[] repeatList(ListObject list, int count) {
    int size = (int) list.getArraySize();
    int finalSize = size * count;

    Object[] objects = new Object[finalSize];

    for(int i = 0; i < count; i++) {
      System.arraycopy(list.items, 0, objects, i * size, size);
    }

    return objects;
  }

  @Specialization(limit = "3")
  protected Object doObjects(BladeObject left, BladeObject right, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride("*", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this, "*", left, right);
  }
}
