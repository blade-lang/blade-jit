package org.nimbus.language.parser;

public record Token(TokenType type, String literal, int line, int offset) {

  @Override
  public String toString() {
    return String.format("<ast::Token type=%s literal='%s' line=%d offset=%d>", this.type, this.literal, this.line, this.offset);
  }

  public Token copyToType(TokenType type, String literal) {
    return new Token(type, literal, this.line, this.offset);
  }
}
