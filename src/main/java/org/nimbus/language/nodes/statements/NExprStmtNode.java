package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;
import org.nimbus.language.runtime.NimNil;

public final class NExprStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode expr;

  @CompilerDirectives.CompilationFinal
  private final boolean discardValue;

  private final SourceSection source;

  public NExprStmtNode(NNode expr, SourceSection source) {
    this(expr, source, false);
  }

  public NExprStmtNode(NNode expr, SourceSection source, boolean discardValue) {
    this.expr = expr;
    this.source = source;
    this.discardValue = discardValue;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object result = expr.execute(frame);
    return discardValue ? NimNil.SINGLETON : result;
  }

  @Override
  public SourceSection getSourceSection() {
    return source;
  }
}
