package org.nimbus.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ExportLibrary(InteropLibrary.class)
public final class NGlobalNamesObject implements TruffleObject {
  private final List<String> names;

  public NGlobalNamesObject(Set<String> names) {
    this.names = new ArrayList<>(names);
  }

  @ExportMessage
  boolean hasArrayElements() {
    return true;
  }

  @ExportMessage
  long getArraySize() {
    return names.size();
  }

  @ExportMessage
  boolean isArrayElementReadable(long index) {
    return index >= 0 && index < names.size();
  }

  @ExportMessage
  Object readArrayElement(long index) throws InvalidArrayIndexException {
    if (!this.isArrayElementReadable(index)) {
      throw InvalidArrayIndexException.create(index);
    }
    return names.get((int) index);
  }
}
