package org.blade.language.builtins;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.blade.language.BaseBuiltinDeclaration;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.runtime.BladeNil;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.RangeObject;
import org.blade.utility.RegulatedMap;

public final class RangeMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("@key", false, RangeMethodsFactory.NKeyDecoratorNodeFactory.getInstance());
      add("@value", false, RangeMethodsFactory.NValueDecoratorNodeFactory.getInstance());
      add("within", false, RangeMethodsFactory.NWithinMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NKeyDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(RangeObject range, Object item) {
      long lower = range.lower;
      long upper = range.upper;

      if (lower == upper) {
        return BladeNil.SINGLETON;
      } else if (item == BladeNil.SINGLETON) {
        return 0L;
      }

      long dimension = range.range;
      if (item instanceof Long index) {
        if (index < dimension - 1) {
          return index + 1;
        }

        return BladeNil.SINGLETON;
      }

      return doFallback(range, item);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      throw BladeRuntimeError.valueError(this, "Ranges are numerically indexed");
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
      return BladeNil.SINGLETON;
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
}
