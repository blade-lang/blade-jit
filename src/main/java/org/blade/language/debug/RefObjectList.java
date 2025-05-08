package org.blade.language.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public final class RefObjectList implements TruffleObject {
  @CompilerDirectives.CompilationFinal(dimensions = 1)
  private final RefObject[] references;

  public RefObjectList(RefObject[] references) {
    this.references = references;
  }

  @ExportMessage
  boolean hasArrayElements() {
    return true;
  }

  @ExportMessage
  long getArraySize() {
    return references.length;
  }

  @ExportMessage
  boolean isArrayElementReadable(long index) {
    return index >= 0 && index < references.length;
  }

  @ExportMessage
  Object readArrayElement(long index) throws InvalidArrayIndexException {
    if (isArrayElementReadable(index)) {
      return references[(int) index];
    } else {
      throw InvalidArrayIndexException.create(index);
    }
  }
}
