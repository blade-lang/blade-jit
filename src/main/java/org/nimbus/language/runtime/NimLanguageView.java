package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.nimbus.language.NimbusLanguage;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@ExportLibrary(value = InteropLibrary.class, delegateTo = "delegate")
@SuppressWarnings("static-method")
public final class NimLanguageView implements TruffleObject {

  final Object delegate;

  NimLanguageView(Object delegate) {
    this.delegate = delegate;
  }

  @ExportMessage
  boolean hasLanguage() {
    return true;
  }

  @ExportMessage
  Class<? extends TruffleLanguage<?>> getLanguage() {
    return NimbusLanguage.class;
  }

  @ExportMessage
  @ExplodeLoop
  boolean hasMetaObject(@CachedLibrary("this.delegate") InteropLibrary interop) {
    for (NimType type : NimType.PRECEDENCE) {
      if (type.isInstance(delegate, interop)) {
        return true;
      }
    }
    return false;
  }

  @ExportMessage
  @ExplodeLoop
  Object getMetaObject(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
    for (NimType type : NimType.PRECEDENCE) {
      if (type.isInstance(delegate, interop)) {
        return type;
      }
    }
    throw UnsupportedMessageException.create();
  }

  @ExportMessage
  @ExplodeLoop
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects, @CachedLibrary("this.delegate") InteropLibrary interop) {
    for (NimType type : NimType.PRECEDENCE) {
      if (type.isInstance(this.delegate, interop)) {
        try {
          if (type == NimType.NUMBER) {
            return numberToString(interop.asInt(delegate));
          } else if (type == NimType.BOOLEAN) {
            return Boolean.toString(interop.asBoolean(delegate));
          } else if (type == NimType.STRING || type == NimType.LIST) {
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
  private static String numberToString(int l) {
    return Integer.toString(l);
  }

  @CompilerDirectives.TruffleBoundary
  private static String numberToString(double l) {
    return Double.toString(l);
  }

  public static Object create(Object value) {
    assert isPrimitiveOrFromOtherLanguage(value);
    return new NimLanguageView(value);
  }

  private static boolean isPrimitiveOrFromOtherLanguage(Object value) {
    InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
    try {
      return !interop.hasLanguage(value) || interop.getLanguage(value) != NimbusLanguage.class;
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
      if (lib.hasLanguage(value) && lib.getLanguage(value) == NimbusLanguage.class) {
        return value;
      } else {
        return create(value);
      }
    } catch (UnsupportedMessageException e) {
      throw shouldNotReachHere(e);
    }
  }
}
