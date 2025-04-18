package org.nimbus.language.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;

public final class NDynamicObjectRefNode extends NNode {
  private final DynamicObject object;

  public NDynamicObjectRefNode(DynamicObject object) {
    this.object = object;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return object;
  }
}
