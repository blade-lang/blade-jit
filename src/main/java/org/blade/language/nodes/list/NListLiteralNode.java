package org.blade.language.nodes.list;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeContext;
import org.blade.language.runtime.ListObject;
import org.blade.language.shared.BuiltinClassesModel;

import java.util.List;

public final class NListLiteralNode extends NNode {

  @Children private final NNode[] items;

  public NListLiteralNode(List<NNode> items) {
    this.items = items.toArray(new NNode[0]);
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    Object[] objects = new Object[items.length];
    for(int i = 0; i < items.length; i++) {
      objects[i] = items[i].execute(frame);
    }

    BuiltinClassesModel classesModel = BladeContext.get(this).objectsModel;
    return new ListObject(classesModel.listShape, classesModel.listObject, objects);
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return items.length > 0;
  }
}
