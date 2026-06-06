package org.zuri.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;
import org.zuri.language.nodes.common.NGlobalScopeObjectNode;
import org.zuri.language.nodes.NNode;
import org.zuri.language.runtime.ZuriRuntimeError;

@NodeChild(value = "globalScopeNode", type = NGlobalScopeObjectNode.class)
@NodeChild(value = "expr")
@NodeField(name = "name", type = String.class)
public abstract class NGlobalAssignExprNode extends NNode {
  abstract protected String getName();

  @Specialization(limit = "3")
  protected Object assign(DynamicObject globalScope, Object value,
                          @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    String name = getName();

    Property property = objectLibrary.getProperty(globalScope, name);
    if (property == null) {
      throw ZuriRuntimeError.error(this, "'", name, "' is not defined in this scope");
    } else if (property.getFlags() == 1) {
      throw ZuriRuntimeError.error(this, "Assignment to constant variable '", name, "'");
    }

    objectLibrary.put(globalScope, name, value);
    return value;
  }
}
