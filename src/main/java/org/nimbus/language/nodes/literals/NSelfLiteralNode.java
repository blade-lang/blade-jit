package org.nimbus.language.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import org.nimbus.language.nodes.NNode;

public final class NSelfLiteralNode extends NNode {
  @Override
  public Object execute(VirtualFrame frame) {
    return frame.getArguments()[0];
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return true;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.ReadVariableTag.class;
  }
}
