package org.blade.language.builtins;

import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.blade.language.BaseBuiltinDeclaration;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.runtime.ListObject;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.utility.RegulatedMap;

public final class ListMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("append", false, ListMethodsFactory.NListAppendMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NListAppendMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(DynamicObject self, Object item,
                           @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary,
                           @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      // strings only have the 'length' item
      try {
        ListObject list = (ListObject) self;
        long size = list.getArraySize();
        list.resize((int)size + 1, objectLibrary);
        interopLibrary.writeArrayElement(self, size, item);
      } catch (UnsupportedMessageException | UnsupportedTypeException | InvalidArrayIndexException e) {
        throw BladeRuntimeError.create(e.getMessage());
      }
      return item;
    }
  }
}
