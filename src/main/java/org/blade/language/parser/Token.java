package org.blade.language.parser;

public record Token(TokenType type, String literal, int line, int offset, int length) {

  @Override
  public String toString() {
    return String.format("<ast::Token type=%s literal='%s' line=%d offset=%d length=%d>", this.type, this.literal, this.line, this.offset, this.length);
  }

  public Token copyToType(TokenType type, String literal) {
    return new Token(type, literal, this.line, this.offset, this.length);
  }
}
