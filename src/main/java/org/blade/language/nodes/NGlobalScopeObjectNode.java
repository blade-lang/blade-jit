package org.blade.language.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

@SuppressWarnings("truffle-inlining")
public abstract class NGlobalScopeObjectNode extends NNode {

  public static NGlobalScopeObjectNode getUncached() {
    return NGlobalScopeObjectNodeGen.getUncached();
  }

  @Specialization
  protected DynamicObject getGlobalScope() {
    return languageContext().globalScope;
  }
}
