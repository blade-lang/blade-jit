package org.nimbus.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NimContext;

import java.util.List;

public final class NListLiteralNode extends NNode {
  @Children private final NNode[] items;

  public NListLiteralNode(List<NNode> items) {
    this.items = items.toArray(new NNode[0]);
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object[] objects = new Object[items.length];
    for(int i = 0; i < items.length; i++) {
      objects[i] = items[i].execute(frame);
    }

    return new NListObject(NimContext.get(this).listShape, objects);
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return items.length > 0;
  }
}
