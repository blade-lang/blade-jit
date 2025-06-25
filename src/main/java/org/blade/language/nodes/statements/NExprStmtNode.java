package org.blade.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BladeNil;

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
    return discardValue ? BladeNil.SINGLETON : result;
  }
}
