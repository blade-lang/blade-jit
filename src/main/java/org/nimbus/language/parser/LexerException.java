package org.nimbus.language.parser;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

@ExportLibrary(InteropLibrary.class)
public class LexerException extends AbstractTruffleException {
  private final Source source;
  private final int line;
  private final int column;
  private final int length;
  private final boolean incompleteSource;

  public LexerException(Source source, int line, int column, int length, boolean incomplete, String message) {
    super(String.format("%s on line %d:%d", message, line, column - source.getLineStartOffset(line)));
    this.source = source;
    this.line = line;
    this.column = column;
    this.length = length;
    this.incompleteSource = incomplete;
  }

  @ExportMessage
  ExceptionType getExceptionType() {
    return ExceptionType.PARSE_ERROR;
  }

  @ExportMessage
  boolean isExceptionIncompleteSource() {
    return incompleteSource;
  }

  @ExportMessage
  boolean hasSourceLocation() {
    return source != null;
  }

  @CompilerDirectives.TruffleBoundary
  @ExportMessage(name = "getSourceLocation")
  SourceSection getSourceSection() throws UnsupportedMessageException {
    if (source == null) {
      throw UnsupportedMessageException.create();
    }
    return source.createSection(line, column, length);
  }
}
