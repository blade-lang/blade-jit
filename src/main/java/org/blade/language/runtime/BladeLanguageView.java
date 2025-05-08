package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.BladeLanguage;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@ExportLibrary(value = InteropLibrary.class, delegateTo = "delegate")
@SuppressWarnings("static-method")
public final class BladeLanguageView implements TruffleObject {

  final Object delegate;

  BladeLanguageView(Object delegate) {
    this.delegate = delegate;
  }

  @ExportMessage
  boolean hasLanguage() {
    return true;
  }

  @ExportMessage
  Class<? extends TruffleLanguage<?>> getLanguage() {
    return BladeLanguage.class;
  }

  @ExportMessage
  @ExplodeLoop
  boolean hasMetaObject(@CachedLibrary("this.delegate") InteropLibrary interop) {
    for (BladeType type : BladeType.PRECEDENCE) {
      if (type.isInstance(delegate, interop)) {
        return true;
      }
    }
    return false;
  }

  @ExportMessage
  @ExplodeLoop
  Object getMetaObject(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
    for (BladeType type : BladeType.PRECEDENCE) {
      if (type.isInstance(delegate, interop)) {
        return type;
      }
    }
    throw UnsupportedMessageException.create();
  }

  @ExportMessage
  @ExplodeLoop
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects, @CachedLibrary("this.delegate") InteropLibrary interop) {
    for (BladeType type : BladeType.PRECEDENCE) {
      if (type.isInstance(this.delegate, interop)) {
        try {
          if (type == BladeType.NUMBER) {
            return numberToString(interop.asLong(delegate));
          } else if (type == BladeType.BOOLEAN) {
            return Boolean.toString(interop.asBoolean(delegate));
          } else if (type == BladeType.STRING || type == BladeType.LIST) {
            return interop.asString(delegate);
          } else {
            return type.getName();
          }
        } catch (UnsupportedMessageException e) {
          throw shouldNotReachHere(e);
        }
      }
    }

    return "Unknown";
  }

  @CompilerDirectives.TruffleBoundary
  private static String numberToString(long l) {
    return Long.toString(l);
  }

  @CompilerDirectives.TruffleBoundary
  private static String numberToString(double l) {
    return Double.toString(l);
  }

  public static Object create(Object value) {
    assert isPrimitiveOrFromOtherLanguage(value);
    return new BladeLanguageView(value);
  }

  private static boolean isPrimitiveOrFromOtherLanguage(Object value) {
    InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
    try {
      return !interop.hasLanguage(value) || interop.getLanguage(value) != BladeLanguage.class;
    } catch (UnsupportedMessageException e) {
      throw shouldNotReachHere(e);
    }
  }

  @CompilerDirectives.TruffleBoundary
  public static Object forValue(Object value) {
    if (value == null) {
      return null;
    }
    InteropLibrary lib = InteropLibrary.getFactory().getUncached(value);
    try {
      if (lib.hasLanguage(value) && lib.getLanguage(value) == BladeLanguage.class) {
        return value;
      } else {
        return create(value);
      }
    } catch (UnsupportedMessageException e) {
      throw shouldNotReachHere(e);
    }
  }
}
