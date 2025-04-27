package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.nimbus.language.nodes.NGlobalScopeObjectNode;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NString;
import org.nimbus.language.runtime.NimRuntimeError;

@NodeChild(value = "globalScopeNode", type = NGlobalScopeObjectNode.class)
@NodeField(name = "name", type = String.class)
public abstract class NGlobalVarRefExprNode extends NNode {
  protected abstract String getName();

  @Specialization(limit = "3")
  protected Object read(DynamicObject globalScope,
                        @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    String name = getName();

    Object value = objectLibrary.getOrDefault(globalScope, name, null);
    if (value == null) {
      throw NimRuntimeError.create("'", name, "' is not defined in this scope");
    }
    return value;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.ReadVariableTag.class;
  }
}
