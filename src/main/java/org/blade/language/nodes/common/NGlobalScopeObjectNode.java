package org.blade.language.nodes.common;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.blade.language.nodes.NNode;

@SuppressWarnings("truffle-inlining")
public abstract class NGlobalScopeObjectNode extends NNode {
  @Specialization
  protected DynamicObject getGlobalScope() {
    return languageContext().globalScope;
  }
}
