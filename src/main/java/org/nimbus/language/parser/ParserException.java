package org.nimbus.language.parser;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

@ExportLibrary(InteropLibrary.class)
public class ParserException extends AbstractTruffleException {
  private final Source source;
  private final int line;
  private final int offset;
  private final int length;
  private final boolean incompleteSource;

  public ParserException(Source source, Token token, boolean incomplete, String message) {
    super(message);
    this.source = source;
    this.line = token.line();
    this.offset = token.offset();
    this.length = token.literal().length();
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

  @ExportMessage(name = "getSourceLocation")
  SourceSection getSourceSection() throws UnsupportedMessageException {
    if (source == null) {
      throw UnsupportedMessageException.create();
    }
    return source.createSection(line, offset - source.getLineStartOffset(line), length);
  }
}
