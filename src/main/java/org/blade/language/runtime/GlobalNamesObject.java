package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.Set;

@ExportLibrary(InteropLibrary.class)
public final class GlobalNamesObject implements TruffleObject {

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  private final String[] names;

  public GlobalNamesObject(Set<String> names) {
    this.names = names.toArray(new String[0]);
  }

  @ExportMessage
  boolean hasArrayElements() {
    return true;
  }

  @ExportMessage
  long getArraySize() {
    return names.length;
  }

  @ExportMessage
  boolean isArrayElementReadable(long index) {
    return index >= 0 && index < names.length;
  }

  @ExportMessage
  Object readArrayElement(long index) throws InvalidArrayIndexException {
    if (!this.isArrayElementReadable(index)) {
      throw InvalidArrayIndexException.create(index);
    }
    return names[(int) index];
  }
}
