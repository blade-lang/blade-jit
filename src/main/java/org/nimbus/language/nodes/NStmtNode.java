package org.nimbus.language.nodes;

import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

public abstract class NStmtNode extends NNode {
  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.StatementTag.class;
  }
}
