package org.nimbus.language.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

@SuppressWarnings("truffle-inlining")
public abstract class NGlobalScopeObjectNode extends NNode {
  @Specialization
  protected DynamicObject getGlobalScope() {
    return languageContext().globalScope;
  }
}
