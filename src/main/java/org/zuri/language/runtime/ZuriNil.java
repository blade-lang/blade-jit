package org.zuri.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.utilities.TriState;
import org.zuri.annotations.ObjectName;

@ExportLibrary(InteropLibrary.class)
@ObjectName("nil")
public class ZuriNil implements TruffleObject {
  public static final ZuriNil SINGLETON = new ZuriNil();
  private static final int IDENTITY_HASH = System.identityHashCode(SINGLETON);

  // disallow instantiation from outside the class
  private ZuriNil() {
  }

  @ExportMessage
  static TriState isIdenticalOrUndefined(@SuppressWarnings("unused") ZuriNil receiver, Object other) {
    return TriState.valueOf(SINGLETON == other);
  }

  @ExportMessage
  static int identityHashCode(@SuppressWarnings("unused") ZuriNil receiver) {
    return IDENTITY_HASH;
  }

  @ExportMessage
  boolean isNull() {
    return true;
  }

  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return "nil";
  }

  @Override
  public String toString() {
    return "nil";
  }
}
