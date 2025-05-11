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

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@ImportStatic(Integer.class)
public abstract class NMultiplyNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLongs(long left, long right) {
    return Math.multiplyExact(left, right);
  }

  @Specialization(replaces = "doLongs")
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigInts(BigIntObject left, BigIntObject right) {
    return new BigIntObject(left.get().multiply(right.get()));
  }

  @Specialization(replaces = "doBigInts", guards = {"leftLibrary.fitsInBigInteger(left)", "rightLibrary.fitsInBigInteger(right)"}, limit = "3")
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doInteropBigInteger(Object left, Object right,
                                                 @CachedLibrary("left") InteropLibrary leftLibrary,
                                                 @CachedLibrary("right") InteropLibrary rightLibrary) {
    try {
      return new BigIntObject(leftLibrary.asBigInteger(left).multiply(rightLibrary.asBigInteger(right)));
    } catch (UnsupportedMessageException e) {
      throw shouldNotReachHere(e);
    }
  }

  @Specialization(replaces = {"doInteropBigInteger"})
  protected double doDoubles(double left, double right) {
    return left * right;
  }

  @Specialization
  protected TruffleString doStringMultiplication(TruffleString string, long count,
                                                 @Cached TruffleString.RepeatNode repeatNode) {
    return repeatNode.execute(string, (int)count, BladeLanguage.ENCODING);
  }

  @Specialization(guards = "count <= MAX_VALUE")
  protected ListObject doListMultiplication(ListObject list, long count) {
    BuiltinClassesModel objectModel = BladeContext.get(this).objectsModel;
    return new ListObject(
      objectModel.listShape,
      objectModel.listObject,
      repeatList(list, count)
    );
  }

  @Specialization(guards = "count > MAX_VALUE")
  protected ListObject doListMultiplicationOutOfBound(ListObject list, long count) {
    throw BladeRuntimeError.create("List multiplication count out of bounds (", count, " > ", Integer.MAX_VALUE, ")");
  }

  @ExplodeLoop
  private Object[] repeatList(ListObject list, long count) {
    int size = (int) list.getArraySize();
    int finalSize = (int)(size * count);

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
