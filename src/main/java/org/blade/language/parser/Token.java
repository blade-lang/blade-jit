package org.blade.language.parser;

public record Token(TokenType type, String literal, int line, int offset, int length) {

  @Override
  public String toString() {
    return String.format(
      "<ast::Token type=%s literal='%s' line=%d offset=%d length=%d>",
      type,
      literal,
      line,
      offset,
      length
    );
  }

  public Token copyToType(TokenType type, String literal) {
    return new Token(type, literal, line, offset, length);
  }

  public Token copyToType(TokenType type) {
    return new Token(type, literal, line, offset, length);
  }
}
