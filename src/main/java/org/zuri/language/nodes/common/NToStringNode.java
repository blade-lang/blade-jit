package org.zuri.language.nodes.common;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.nodes.ZuriTypes;
import org.zuri.language.runtime.*;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@TypeSystemReference(ZuriTypes.class)
@GenerateUncached
@GenerateInline
@GenerateCached(false)
public abstract class NToStringNode extends Node {
  static final int LIMIT = 5;

  private static final TruffleString NIL = ZString.toTruffleString("nil");
  private static final TruffleString TRUE = ZString.toTruffleString("true");
  private static final TruffleString FALSE = ZString.toTruffleString("false");
  private static final TruffleString FOREIGN_OBJECT = ZString.toTruffleString("[foreign object]");

  public abstract TruffleString execute(Node node, Object value);

  @Specialization
  public static TruffleString fromNil(@SuppressWarnings("unused") ZuriNil value) {
    return NIL;
  }

  @Specialization
  protected static TruffleString fromString(String value,
                                            @Cached.Shared("fromJava") @Cached(inline = false) TruffleString.FromJavaStringNode fromJavaStringNode) {
    return ZString.fromJavaString(fromJavaStringNode, value);
  }

  @Specialization
  public static TruffleString fromTruffleString(TruffleString value) {
    return value;
  }

  @Specialization
  public static TruffleString fromBoolean(boolean value) {
    return value ? TRUE : FALSE;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static TruffleString fromLong(long value,
                                          @Cached.Shared("fromLong") @Cached(inline = false) TruffleString.FromLongNode fromLongNode) {
    return ZString.fromLong(fromLongNode, value);
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  protected static TruffleString fromDouble(double value,
                                          @Cached.Shared("fromJava") @Cached(inline = false) TruffleString.FromJavaStringNode fromJavaStringNode) {
    return ZString.fromObject(fromJavaStringNode, value);
  }

  @Specialization
  protected static TruffleString fromBigNumber(BigIntObject value,
                                               @Cached.Shared("fromJava") @Cached(inline = false) TruffleString.FromJavaStringNode fromJavaStringNode) {
    return ZString.fromObject(fromJavaStringNode, value.get());
  }

  @Specialization
  public static TruffleString fromFunction(FunctionObject value,
                                           @Cached.Shared("fromJava") @Cached(inline = false) TruffleString.FromJavaStringNode fromJavaStringNode) {
    return ZString.fromJavaString(fromJavaStringNode, value.name);
  }

  @Specialization
  protected static TruffleString fromObject(ZuriObject value,
                                                @Cached.Shared("fromJava") @Cached(inline = false) TruffleString.FromJavaStringNode fromJavaStringNode) {
    return ZString.fromObject(fromJavaStringNode, value);
  }

  @Specialization(limit = "LIMIT")
  public static TruffleString fromInterop(Object value,
                                          @CachedLibrary("value") InteropLibrary interop,
                                          @Cached.Shared("fromLong") @Cached(inline = false) TruffleString.FromLongNode fromLongNode,
                                          @Cached.Shared("fromJava") @Cached(inline = false) TruffleString.FromJavaStringNode fromJavaStringNode) {
    try {
      if (interop.fitsInLong(value)) {
        return ZString.fromLong(fromLongNode, interop.asLong(value));
      } else if (interop.fitsInDouble(value)) {
        return ZString.fromObject(fromJavaStringNode, interop.asDouble(value));
      } else if (interop.isString(value)) {
        return ZString.fromJavaString(fromJavaStringNode, interop.asString(value));
      } else if (interop.isNumber(value) && value instanceof BigIntObject bigIntObject) {
        return ZString.fromObject(fromJavaStringNode, bigIntObject.get());
      } else if (interop.isNull(value)) {
        return NIL;
      } else {
        return FOREIGN_OBJECT;
      }
    } catch (UnsupportedMessageException e) {
      throw shouldNotReachHere(e);
    }
  }
}
