package org.nimbus.language.nodes.expressions.logical;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NUnaryNode;

public class NLogicalNotNode extends NUnaryNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode value;

  public NLogicalNotNode(NNode value) {
    this.value = value;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return !value.executeBoolean(frame);
  }
}
