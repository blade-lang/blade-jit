package org.blade.language.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BaseBuiltinDeclaration;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.nodes.string.NReadStringPropertyNode;
import org.blade.language.runtime.BString;
import org.blade.utility.RegulatedMap;

public class ObjectMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("to_string", false, ObjectMethodsFactory.NObjectToStringMethodNodeFactory.getInstance());
      add("has_prop", false, ObjectMethodsFactory.NObjectHasPropMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NObjectToStringMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doObject(DynamicObject self,
                                     @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      return BString.fromObject(fromJavaStringNode, self);
    }

    @Fallback
    protected Object doPrimitive(Object self) {
      return BString.fromObject(self);
    }
  }

  public abstract static class NObjectHasPropMethodNode extends NBuiltinFunctionNode {
    @Specialization(limit = "3")
    protected boolean doObject(DynamicObject self, Object property,
                               @CachedLibrary("self") DynamicObjectLibrary dynamicObjectLibrary) {
      return dynamicObjectLibrary.containsKey(self, BString.toString(property));
    }

    @Specialization
    protected boolean doString(TruffleString self, Object property) {
      // strings only have the 'length' property
      return NReadStringPropertyNode.LENGTH_PROP.equals(BString.toString(property));
    }

    @Fallback
    protected boolean doPrimitive(Object self, Object property) {
      // primitives don't own any properties
      return false;
    }
  }
}
