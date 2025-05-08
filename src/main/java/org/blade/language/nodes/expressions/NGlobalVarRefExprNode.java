package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.blade.language.nodes.NGlobalScopeObjectNode;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeRuntimeError;

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
      throw BladeRuntimeError.create("'", name, "' is not defined in this scope");
    }
    return value;
  }
}
