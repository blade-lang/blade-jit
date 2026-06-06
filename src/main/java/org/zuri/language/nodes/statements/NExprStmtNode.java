package org.zuri.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.NStmtNode;
import org.zuri.language.runtime.ZuriNil;

public final class NExprStmtNode extends NStmtNode {
  @CompilerDirectives.CompilationFinal
  public final boolean discardValue;
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode expr;

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
    return discardValue ? ZuriNil.SINGLETON : result;
  }
}
