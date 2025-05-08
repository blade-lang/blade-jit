package org.blade.language.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.runtime.BladeContext;

@TypeSystemReference(BladeTypes.class)
public abstract class NBaseNode extends Node {
  protected final BladeContext languageContext() {
    return BladeContext.get(this);
  }
}
