package org.blade.language.builtins;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BaseBuiltinDeclaration;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.nodes.string.NReadStringPropertyNode;
import org.blade.language.nodes.util.NToBooleanNode;
import org.blade.language.runtime.*;
import org.blade.language.shared.BuiltinClassesModel;
import org.blade.utility.RegulatedMap;

public class ObjectMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("to_string", false, ObjectMethodsFactory.NObjectToStringMethodNodeFactory.getInstance());
      add("has_prop", false, ObjectMethodsFactory.NObjectHasPropMethodNodeFactory.getInstance());
      add("get_class", false, ObjectMethodsFactory.GetClassMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NObjectToStringMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doString(TruffleString self) {
      return self;
    }

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

  @ImportStatic(BladeContext.class)
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
    protected boolean doPrimitive(Object self, Object property, @Bind Node node,
                                  @Cached NToBooleanNode toBooleanNode,
                                  @Cached("get(node)") BladeContext context,
                                  @Cached(value = "context.objectsModel.objectObject") DynamicObject clasObject,
                                  @CachedLibrary(value = "context.objectsModel.objectObject") InteropLibrary classInteropLibrary) {
      try {
        return toBooleanNode.executeBoolean(classInteropLibrary.readMember(clasObject, BString.toString(property)));
      } catch (UnsupportedMessageException | UnknownIdentifierException e) {
        return false;
      }
    }
  }

  @ImportStatic(BladeContext.class)
  public abstract static class GetClassMethodNode extends NBuiltinFunctionNode {

    @Specialization
    protected Object doInt(int object, @Bind Node node,
                           @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel objectsModel,
                           @Cached(value = "objectsModel.numberObject", neverDefault = true) BladeClass numberObject) {
      return numberObject;
    }

    @Specialization
    protected Object doLong(long object, @Bind Node node,
                            @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel objectsModel,
                            @Cached(value = "objectsModel.numberObject", neverDefault = true) BladeClass numberObject) {
      return numberObject;
    }

    @Specialization
    protected Object doDouble(double object, @Bind Node node,
                              @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel objectsModel,
                              @Cached(value = "objectsModel.numberObject", neverDefault = true) BladeClass numberObject) {
      return numberObject;
    }

    @Specialization
    protected Object doBoolean(boolean object, @Bind Node node,
                               @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel objectsModel,
                               @Cached(value = "objectsModel.booleanObject", neverDefault = true) BladeClass booleanObject) {
      return booleanObject;
    }

    @Specialization
    protected Object doBigInt(BigIntObject object, @Bind Node node,
                              @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel objectsModel,
                              @Cached(value = "objectsModel.bigIntObject", neverDefault = true) BladeClass bigIntObject) {
      return bigIntObject;
    }

    @Specialization
    protected Object doString(TruffleString object, @Bind Node node,
                              @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel objectsModel,
                              @Cached(value = "objectsModel.stringObject", neverDefault = true) BladeClass stringObject) {
      return stringObject;
    }

    @Specialization
    protected Object doRange(RangeObject object, @Bind Node node,
                             @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel objectsModel,
                             @Cached(value = "objectsModel.rangeObject", neverDefault = true) BladeClass rangeObject) {
      return rangeObject;
    }

    @Specialization
    protected Object doObject(BladeObject object) {
      return object.classObject;
    }

    @Fallback
    protected Object doOthers(Object object, @Bind Node node,
                              @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel objectsModel) {
      return objectsModel.objectObject;
    }
  }
}
