package org.nimbus.language.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NBaseBuiltinDeclaration;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.nodes.string.NReadStringPropertyNode;
import org.nimbus.language.runtime.NString;
import org.nimbus.utility.RegulatedMap;

public class NObjectMethods implements NBaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("to_string", false, NObjectMethodsFactory.NObjectToStringMethodNodeFactory.getInstance());
      add("has_prop", false, NObjectMethodsFactory.NObjectHasPropMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NObjectToStringMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doObject(DynamicObject self,
                                     @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      return NString.fromObject(fromJavaStringNode, self);
    }

    @Fallback
    protected Object doPrimitive(Object self) {
      return NString.fromObject(self);
    }
  }

  public abstract static class NObjectHasPropMethodNode extends NBuiltinFunctionNode {
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
}
