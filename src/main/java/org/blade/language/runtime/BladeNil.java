package org.blade.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.utilities.TriState;
import org.blade.annotations.ObjectName;

@ExportLibrary(InteropLibrary.class)
@ObjectName("Nil")
public class BladeNil implements TruffleObject {
  public static final BladeNil SINGLETON = new BladeNil();
  private static final int IDENTITY_HASH = System.identityHashCode(SINGLETON);

  // disallow instantiation from outside the class
  private BladeNil() {
  }

  @ExportMessage
  boolean isNull() {
    return true;
  }

  @ExportMessage
  static TriState isIdenticalOrUndefined(@SuppressWarnings("unused") BladeNil receiver, Object other) {
    return TriState.valueOf(SINGLETON == other);
  }

  @ExportMessage
  static int identityHashCode(@SuppressWarnings("unused") BladeNil receiver) {
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
