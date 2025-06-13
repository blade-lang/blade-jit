package org.blade.language.nodes.list;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.Shape;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeClass;
import org.blade.language.runtime.BladeContext;
import org.blade.language.runtime.ListObject;
import org.blade.language.shared.BuiltinClassesModel;

import java.util.List;

public final class NListLiteralNode extends NNode {

  @Children
  private final NNode[] items;

  private final int length;

  private final BuiltinClassesModel classesModel = BladeContext.get(this).objectsModel;
  private final Shape listShape = classesModel.listShape;
  private final BladeClass listClass = classesModel.listObject;

  public NListLiteralNode(List<NNode> items) {
    this.items = items.toArray(new NNode[0]);
    this.length = items.size();
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    Object[] objects = new Object[length];
    for (int i = 0; i < length; i++) {
      objects[i] = items[i].execute(frame);
    }

    return new ListObject(listShape, listClass, objects);
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return length > 0;
  }
}
