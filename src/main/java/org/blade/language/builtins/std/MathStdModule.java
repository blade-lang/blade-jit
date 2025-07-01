package org.blade.language.builtins.std;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import org.blade.language.BaseBuiltinDeclaration;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.runtime.*;
import org.blade.utility.RegulatedMap;

public final class MathStdModule implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("abs", false, MathStdModuleFactory.AbsFunctionNodeFactory.getInstance());
    }};
  }

  public abstract static class AbsFunctionNode extends NBuiltinFunctionNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected long doLong(long arg) {
      return arg < 0 ? Math.negateExact(arg) : arg;
    }

    @Specialization(replaces = "doLong")
    protected double doDouble(double arg) {
      return Math.abs(arg);
    }

    @Specialization
    protected Object doObject(BladeObject arg, @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      return methodOverride(this, "@abs", arg, interopLibrary, Double.NaN);
    }

    @Fallback
    protected double notANumber(Object object) {
      return Double.NaN;
    }
  }
}
