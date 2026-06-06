package org.zuri.language.builtins;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import org.zuri.language.BaseBuiltinDeclaration;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.language.runtime.ZuriClass;
import org.zuri.language.runtime.ZuriNil;
import org.zuri.language.runtime.ZuriRuntimeError;
import org.zuri.language.runtime.ListObject;
import org.zuri.utility.RegulatedMap;

public final class ListMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("@key", false, ListMethodsFactory.NKeyDecoratorNodeFactory.getInstance());
      add("@value", false, ListMethodsFactory.NValueDecoratorNodeFactory.getInstance());
      add("length", false, ListMethodsFactory.NLengthMethodNodeFactory.getInstance());
      add("append", false, ListMethodsFactory.NAppendMethodNodeFactory.getInstance());
      add("clear", false, ListMethodsFactory.NClearMethodNodeFactory.getInstance());
      add("clone", false, ListMethodsFactory.NCloneMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NKeyDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(ListObject list, Object item) {
      Object[] items = list.getItems();
      int length = (int)list.getArraySize();
      if (length == 0) {
        return ZuriNil.SINGLETON;
      } else if (item == ZuriNil.SINGLETON) {
        return 0L;
      }

      if (item instanceof Long index) {
        if (index < length - 1) {
          return index + 1;
        }

        return ZuriNil.SINGLETON;
      }

      throw ZuriRuntimeError.valueError(this, "Lists are numerically indexed");
    }
  }

  public abstract static class NValueDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(ListObject list, long index) {
      Object[] items = list.getItems();
      if (index > -1 && index < list.getArraySize()) {
        return items[(int) index];
      }

      return doFallback(list, index);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      return ZuriNil.SINGLETON;
    }
  }

  public abstract static class NLengthMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(ListObject self) {
      return self.getArraySize();
    }
  }

  public abstract static class NAppendMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(ListObject list, Object item,
                           @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      try {
        long size = list.getArraySize();
        list.resize(size + 1);
        interopLibrary.writeArrayElement(list, size, item);
      } catch (UnsupportedMessageException | UnsupportedTypeException | InvalidArrayIndexException e) {
        throw ZuriRuntimeError.error(this, e.getMessage());
      }

      return item;
    }
  }

  public abstract static class NClearMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(ListObject self) {
      self.resize(0);
      return ZuriNil.SINGLETON;
    }
  }

  public abstract static class NCloneMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(ListObject self) {
      int length = (int) self.getArraySize();
      Object[] items = self.getItems();

      Object[] objects = new Object[length];
      System.arraycopy(items, 0, objects, 0, length);

      return new ListObject(self.getShape(), (ZuriClass) self.classObject, objects);
    }
  }
}

