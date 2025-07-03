package org.blade.language.runtime;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.blade.language.nodes.BladeTypesGen;
import org.blade.language.nodes.functions.NMethodDispatchNode;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;

@ExportLibrary(InteropLibrary.class)
public final class FunctionObject extends BladeObject {
  public static final int INLINE_CACHE_SIZE = 2;

  public final String name;
  public final int argumentsCount;
  public final boolean variadic;
  private final CyclicAssumption callTargetStable;

  public RootCallTarget callTarget;

  public FunctionObject(Shape shape, BladeClass classObject, String name, RootCallTarget target, int argumentsCount) {
    this(shape, classObject, name, target, argumentsCount, false);
  }

  public FunctionObject(Shape shape, BladeClass classObject, String name, RootCallTarget target, int argumentsCount, boolean variadic) {
    super(shape, classObject);
    callTarget = target;
    this.name = name;
    this.argumentsCount = argumentsCount;
    this.variadic = variadic;
    callTargetStable = new CyclicAssumption(name);
  }

  public Assumption getCallTargetStable() {
    return callTargetStable.getAssumption();
  }

  public void setCallTarget(RootCallTarget callTarget) {
    boolean wasNull = this.callTarget == null;
    this.callTarget = callTarget;
    if (!wasNull) {
      callTargetStable.invalidate();
    }
  }

  public CallTarget getCallTarget() {
    return callTarget;
  }

  @ExportMessage
  boolean isExecutable() {
    return true;
  }

  @ExportMessage
  boolean isNull() {
    return false;
  }

  @CompilerDirectives.TruffleBoundary
  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return toString();
  }

  @Override
  public String toString() {
    String format = variadic ? "<function %s(%d...) at 0x%x>" : "<function %s(%d) at 0x%x>";
    return BString.format(format, name, argumentsCount, callTarget.hashCode());
  }

  @ReportPolymorphism
  @ExportMessage
  abstract static class Execute {

    @Specialization(
      limit = "INLINE_CACHE_SIZE",
      guards = "function.getCallTarget() == cachedTarget",
      assumptions = "callTargetStable"
    )
    @SuppressWarnings("unused")
    protected static Object doDirect(FunctionObject function, Object[] arguments,
                                     @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                     @Cached("function.callTarget") CallTarget cachedTarget,
                                     @Cached("create(cachedTarget)") DirectCallNode callNode) {
      return callNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(FunctionObject function, Object[] arguments,
                                       @Cached IndirectCallNode callNode) {
      return callNode.call(function.callTarget, arguments);
    }
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  static int identityHashCode(FunctionObject receiver) {
    return System.identityHashCode(receiver);
  }

  private boolean isRemValue(Object value) {
    return BladeTypesGen.isImplicitLong(value) ||
      BladeTypesGen.isImplicitDouble(value) ||
      BladeTypesGen.isBoolean(value) ||
      value == BladeNil.SINGLETON ||
      value instanceof String ||
      value instanceof TruffleString ||
      value instanceof BladeObject;
  }
}
