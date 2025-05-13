package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public record MemberNamesObject(
  @CompilerDirectives.CompilationFinal(dimensions = 1) Object[] names) implements TruffleObject {

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
  public Object readArrayElement(long index) throws InvalidArrayIndexException {
    if (!isArrayElementReadable(index)) {
      throw InvalidArrayIndexException.create(index);
    }
    return names[(int) index];
  }

  public Object[] getNames() {
    return names;
  }
}
