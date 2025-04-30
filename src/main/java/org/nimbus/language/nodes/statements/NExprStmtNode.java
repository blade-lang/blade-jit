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
  public final boolean discardValue;

  public NExprStmtNode(NNode expr) {
    this(expr, false);
  }

  public NExprStmtNode(NNode expr, boolean discardValue) {
    this.expr = expr;
    this.discardValue = discardValue;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object result = expr.execute(frame);
    return discardValue ? NimNil.SINGLETON : result;
  }
}
