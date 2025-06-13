package org.blade.language.debug;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.source.SourceSection;

@ExportLibrary(InteropLibrary.class)
public abstract class RefObject implements TruffleObject {
  public abstract Object read(Frame frame);

  public abstract void write(Frame frame, Object value);

  protected final String name;
  private final SourceSection sourceSection;

  public RefObject(String name, SourceSection sourceSection) {
    this.name = name;
    this.sourceSection = sourceSection;
  }

  @ExportMessage
  boolean isString() {
    return true;
  }

  @ExportMessage
  String asString() {
    return name;
  }

  @ExportMessage
  boolean hasSourceLocation() {
    return sourceSection != null;
  }

  @ExportMessage
  SourceSection getSourceLocation() {
    return sourceSection;
  }
}
