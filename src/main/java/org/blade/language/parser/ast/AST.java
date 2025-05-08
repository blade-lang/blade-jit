package org.blade.language.parser.ast;

public abstract class AST {
  public int startLine = 1;
  public int endLine = 1;
  public int startColumn = 0;
  public int endColumn = 0;
  public boolean wrapped = false;
}
