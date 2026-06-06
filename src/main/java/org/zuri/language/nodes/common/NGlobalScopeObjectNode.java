package org.zuri.language.nodes.common;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.zuri.language.nodes.NNode;

@SuppressWarnings("truffle-inlining")
public abstract class NGlobalScopeObjectNode extends NNode {
  @Specialization
  protected DynamicObject getGlobalScope() {
    return languageContext().globalScope;
  }
}
