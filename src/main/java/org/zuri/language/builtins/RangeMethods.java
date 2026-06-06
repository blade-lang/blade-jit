package org.zuri.language.builtins;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import org.zuri.language.BaseBuiltinDeclaration;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.language.runtime.*;
import org.zuri.language.shared.BuiltinClassesModel;
import org.zuri.utility.RegulatedMap;

public final class RangeMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("@key", false, RangeMethodsFactory.NKeyDecoratorNodeFactory.getInstance());
      add("@value", false, RangeMethodsFactory.NValueDecoratorNodeFactory.getInstance());
      add("within", false, RangeMethodsFactory.NWithinMethodNodeFactory.getInstance());
      add("to_list", false, RangeMethodsFactory.NToListMethodFactory.getInstance());
    }};
  }

  public abstract static class NKeyDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(RangeObject range, Object item) {
      long lower = range.lower;
      long upper = range.upper;

      if (lower == upper) {
        return ZuriNil.SINGLETON;
      } else if (item == ZuriNil.SINGLETON) {
        return 0L;
      }

      long dimension = range.range;
      if (item instanceof Long index) {
        if (index < dimension - 1) {
          return index + 1;
        }

        return ZuriNil.SINGLETON;
      }

      return doFallback(range, item);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      throw ZuriRuntimeError.valueError(this, "Ranges are numerically indexed");
    }
  }

  public abstract static class NValueDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(RangeObject range, long index) {
      long lower = range.lower;
      long upper = range.upper;
      long dimension = range.range;
      if (index > -1 && index < dimension) {
        return upper > lower ? lower + index : lower - index;
      }

      return doFallback(lower, index);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      return ZuriNil.SINGLETON;
    }
  }

  public abstract static class NWithinMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(RangeObject range, long value,
                              @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary,
                              @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {

      long lower = range.lower;
      long upper = range.upper;

      return lower > upper ? (
        value <= lower && value >= upper
      ) : (
        value >= lower && value <= upper
      );
    }

    @Fallback
    protected Object doInvalid(Object object, Object value) {
      return false;
    }
  }

  @ImportStatic(ZuriContext.class)
  public abstract static class NToListMethod extends NBuiltinFunctionNode {
    //    @ExplodeLoop
    @Specialization
    protected ListObject toList(RangeObject range, @Bind Node node,
                                @Cached(value = "get(node).objectsModel", neverDefault = true) BuiltinClassesModel classesModel,
                                @Cached(value = "classesModel.listShape", neverDefault = true) Shape listShape,
                                @Cached(value = "classesModel.listObject", neverDefault = true) ZuriClass listObject) {
      long lower = range.lower;
      int length = (int) Math.abs(range.upper - lower);
      Object[] items = new Object[length];

      for (int i = 0; i < length; i++) {
        items[i] = lower + i;
      }

      return new ListObject(listShape, listObject, items);
    }
  }
}
