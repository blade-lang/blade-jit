package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NSharedPropertyWriterNode;

@NodeChild("target")
@NodeChild("value")
@NodeField(name = "name", type = String.class)
public abstract class NSetPropertyNode extends NNode {
  protected abstract String getName();

  @Specialization
  protected Object writeProperty(Object target, Object value,
                                 @Cached NSharedPropertyWriterNode writerNode) {
    return writerNode.executeWrite(target, getName(), value);
  }
}
