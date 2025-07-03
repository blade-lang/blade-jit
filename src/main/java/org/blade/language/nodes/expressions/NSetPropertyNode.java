package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NPropertyWriterNode;

@NodeChild("target")
@NodeChild("value")
@NodeField(name = "name", type = String.class)
public abstract class NSetPropertyNode extends NNode {
  protected abstract String getName();

  @Specialization
  protected Object writeProperty(Object target, Object value,
                                 @Cached NPropertyWriterNode writerNode) {
    return writerNode.executeWrite(target, getName(), value);
  }
}
