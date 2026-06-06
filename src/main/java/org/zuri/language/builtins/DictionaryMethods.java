package org.zuri.language.builtins;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.zuri.language.BaseBuiltinDeclaration;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.language.runtime.ZuriNil;
import org.zuri.language.runtime.DictionaryObject;
import org.zuri.utility.RegulatedMap;

public final class DictionaryMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("@key", false, DictionaryMethodsFactory.NKeyDecoratorNodeFactory.getInstance());
      add("@value", false, DictionaryMethodsFactory.NValueDecoratorNodeFactory.getInstance());
      add("size", false, DictionaryMethodsFactory.NSizeMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NKeyDecoratorNode extends NBuiltinFunctionNode {

    @Specialization
    protected Object doAny(DictionaryObject dictionary, Object key,
                           @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary) {
      Object[] keys = objectLibrary.getKeyArray(dictionary);
      int length = keys.length;
      if (length == 0) {
        return ZuriNil.SINGLETON;
      } else if (key == ZuriNil.SINGLETON) {
        return keys[0];
      }

      return getNextKey(keys, key, length);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      return ZuriNil.SINGLETON;
    }

    //    @ExplodeLoop
    private Object getNextKey(Object[] keys, Object key, int keysLength) {
      int index = 0;
      for (int i = 0; i < keysLength; i++) {
        if (objectEquals(key, keys[i])) {
          index = i;
          break;
        }
      }

      if (index < keysLength - 1) {
        return keys[index + 1];
      }

      return ZuriNil.SINGLETON;
    }
  }

  public abstract static class NValueDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(DictionaryObject dictionary, Object key,
                           @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary) {
      return objectLibrary.getOrDefault(dictionary, key, ZuriNil.SINGLETON);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      return ZuriNil.SINGLETON;
    }
  }

  public abstract static class NSizeMethodNode extends NBuiltinFunctionNode {

    @Specialization
    protected long doAny(DictionaryObject dictionary,
                         @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary) {
      return objectLibrary.getKeyArray(dictionary).length;
    }

    @Fallback
    protected Object doFallback(Object object) {
      return Double.NaN;
    }
  }
}
