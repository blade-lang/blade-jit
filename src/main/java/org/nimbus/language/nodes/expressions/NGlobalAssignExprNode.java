package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;
import org.nimbus.language.nodes.NGlobalScopeObjectNode;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NString;
import org.nimbus.language.runtime.NimRuntimeError;

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
      throw NimRuntimeError.create("'", name, "' is not defined in this scope");
    } else if (property.getFlags() == 1) {
      throw NimRuntimeError.create("Assignment to constant variable '", name, "'");
    }

    objectLibrary.put(globalScope, name, value);
    return value;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.WriteVariableTag.class;
  }
}
