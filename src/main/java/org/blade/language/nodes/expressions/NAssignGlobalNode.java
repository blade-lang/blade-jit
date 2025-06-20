package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeRuntimeError;

@GenerateInline
@GenerateUncached
public abstract class NAssignGlobalNode extends Node {

  public static NAssignGlobalNode getUncached() {
    return NAssignGlobalNodeGen.getUncached();
  }

  @NeverDefault
  public static NAssignGlobalNode create() {
    return NAssignGlobalNodeGen.create();
  }

  public abstract Object executeSet(Node node, Object globals, TruffleString name, Object value);

  @Specialization(limit = "3")
  protected Object assign(Node node, DynamicObject globalScope, TruffleString name, Object value,
                          @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    Property property = objectLibrary.getProperty(globalScope, name);
    if (property == null) {
      throw BladeRuntimeError.error(node, "'", name, "' is not defined in this scope");
    } else if (property.getFlags() == 1) {
      throw BladeRuntimeError.error(node, "Assignment to constant variable '", name, "'");
    }

    objectLibrary.put(globalScope, name, value);
    return value;
  }
}
