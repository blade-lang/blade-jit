package org.nimbus.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.utilities.TriState;

@ExportLibrary(InteropLibrary.class)
public class NimNil implements TruffleObject {
  public static final NimNil SINGLETON = new NimNil();
  private static final int IDENTITY_HASH = System.identityHashCode(SINGLETON);

  // disallow instantiation from outside the class
  private NimNil() {
  }

  @ExportMessage
  boolean isNull() {
    return true;
  }

  @ExportMessage
  static TriState isIdenticalOrUndefined(@SuppressWarnings("unused") NimNil receiver, Object other) {
    return TriState.valueOf(SINGLETON == other);
  }

  @ExportMessage
  static int identityHashCode(@SuppressWarnings("unused") NimNil receiver) {
    return IDENTITY_HASH;
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
