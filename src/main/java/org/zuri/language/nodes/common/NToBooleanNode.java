package org.zuri.language.nodes.common;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.nodes.NNode;
import org.zuri.language.runtime.*;

import java.math.BigInteger;

@NodeChild
public abstract class NToBooleanNode extends NNode {

  public abstract boolean executeBoolean(Object object);

  @Specialization
  public static boolean doBoolean(boolean value) {
    return value;
  }

  @Specialization
  public static boolean doLong(long value) {
    return value > 0;
  }

  @Specialization
  public static boolean doDouble(double value) {
    return value > 0;
  }

  @Specialization
  public static boolean doString(TruffleString value, @Cached TruffleString.CodePointLengthNode lengthNode) {
    return ZString.length(value, lengthNode) > 0;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static boolean doString(BigIntObject value) {
    return value.get().compareTo(BigInteger.ZERO) > 0;
  }

  @Specialization
  public static boolean doString(ListObject value) {
    return value.getArraySize() > 0;
  }

  @Specialization
  public static boolean doString(DictionaryObject value,
                                 @CachedLibrary(limit = "3")DynamicObjectLibrary objectLibrary) {
    return objectLibrary.getKeyArray(value).length > 0;
  }

  @Specialization
  public static boolean doString(RangeObject value) {
    return value.upper != value.lower;
  }

  @Specialization(guards = "lib.isBoolean(value)", limit = "3")
  public static boolean doInterop(Object value,
                                  @Bind Node node,
                                  @CachedLibrary("value") InteropLibrary lib) {
    try {
      return lib.asBoolean(value);
    } catch (UnsupportedMessageException e) {
      return doFallback(value, node);
    }
  }

  @Fallback
  public static boolean doFallback(Object value, @Bind Node node) {
    return true;
  }
}