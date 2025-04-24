package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;

public final class NReturnStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode value;

  private final SourceSection source;

  public NReturnStmtNode(NNode value, SourceSection source) {
    this.value = value;
    this.source = source;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    throw new NReturnException(value.execute(frame));
  }

  @Override
  public SourceSection getSourceSection() {
    return source;
  }
}
