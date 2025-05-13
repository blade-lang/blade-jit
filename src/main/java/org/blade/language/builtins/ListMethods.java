package org.blade.language.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
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
import org.blade.language.runtime.BladeNil;
import org.blade.language.runtime.ListObject;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.utility.RegulatedMap;

public final class ListMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("@key", false, ListMethodsFactory.NKeyDecoratorNodeFactory.getInstance());
      add("@value", false, ListMethodsFactory.NValueDecoratorNodeFactory.getInstance());
      add("append", false, ListMethodsFactory.NAppendMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NKeyDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(ListObject list, Object item,
                           @Cached(value = "list.items", neverDefault = true, dimensions = 1) Object[] items,
                           @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary,
                           @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      int length = items.length;
      if(length == 0) {
        return BladeNil.SINGLETON;
      } else if(item == BladeNil.SINGLETON) {
        return 0L;
      }

      if(item instanceof Long index) {
        if(index < length - 1) {
          return index + 1;
        }

        return BladeNil.SINGLETON;
      }

      return doFallback(list, item);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      throw BladeRuntimeError.valueError(this, "Lists are numerically indexed");
    }
  }

  public abstract static class NValueDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(ListObject list, long index,
                           @Cached(value = "list.items", neverDefault = true, dimensions = 1) Object[] items,
                           @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary,
                           @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      if(index > -1 && index < items.length) {
        return items[(int)index];
      }

      return doFallback(list, index);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      return BladeNil.SINGLETON;
    }
  }

  public abstract static class NAppendMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(DynamicObject self, Object item,
                           @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary,
                           @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
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
