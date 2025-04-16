package org.nimbus.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;

public final class NDoubleLiteralNode extends NNode {
  private final double value;

  public NDoubleLiteralNode(double value) {
    this.value = value;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return value;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return value > 0D;
  }
}
