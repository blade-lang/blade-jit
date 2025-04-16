package org.nimbus.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NListObject;

import java.util.List;

public final class NListLiteralNode extends NNode {
  @Children private final NNode[] items;
  private final Shape shape;

  public NListLiteralNode(Shape shape, List<NNode> items) {
    this.shape = shape;
    this.items = items.toArray(new NNode[0]);
  }

  @Override
  public Object execute(VirtualFrame frame) {
    Object[] objects = new Object[items.length];
    for(int i = 0; i < items.length; i++) {
      objects[i] = items[i].execute(frame);
    }

    return new NListObject(shape, objects);
  }
}
