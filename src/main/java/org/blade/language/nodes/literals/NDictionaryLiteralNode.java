package org.blade.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeContext;
import org.blade.language.runtime.DictionaryObject;
import org.blade.language.shared.BuiltinClassesModel;

import java.util.List;

public final class NDictionaryLiteralNode extends NNode {

  @Children
  private final NNode[] keys;
  @Children
  private final NNode[] values;

  public NDictionaryLiteralNode(List<NNode> keys, List<NNode> values) {
    this.keys = keys.toArray(new NNode[0]);
    this.values = values.toArray(new NNode[0]);
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    int keysLength = keys.length;
    Object[] keysObject = new Object[keysLength];
    Object[] valuesObject = new Object[keysLength];

    for (int i = 0; i < keysLength; i++) {
      keysObject[i] = keys[i].execute(frame);
      valuesObject[i] = values[i].execute(frame);
    }

    BuiltinClassesModel classesModel = BladeContext.get(this).objectsModel;
    return new DictionaryObject(classesModel.dictionaryShape, classesModel.dictionaryObject, keysObject, valuesObject);
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return keys.length > 0;
  }
}
