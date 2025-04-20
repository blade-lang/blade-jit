package org.nimbus.language.builtins.object;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.nodes.string.NReadStringPropertyNode;
import org.nimbus.language.runtime.NString;

public abstract class NObjectHasPropMethodNode extends NBuiltinFunctionNode {
  @Specialization(limit = "3")
  protected boolean doObject(DynamicObject self, Object property,
                             @CachedLibrary("self") DynamicObjectLibrary dynamicObjectLibrary) {
    return dynamicObjectLibrary.containsKey(self, NString.toString(property));
  }

  @Specialization
  protected boolean doString(TruffleString self, Object property) {
    // strings only have the 'length' property
    return NReadStringPropertyNode.LENGTH_PROP.equals(NString.toString(property));
  }

  @Fallback
  protected boolean doPrimitive(Object self, Object property) {
    // primitives don't own any properties
    return false;
  }
}
