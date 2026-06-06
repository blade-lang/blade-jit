package org.zuri.language.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import org.zuri.language.runtime.ZuriContext;

@TypeSystemReference(ZuriTypes.class)
public abstract class NBaseNode extends Node {
  protected final ZuriContext languageContext() {
    return ZuriContext.get(this);
  }
}
