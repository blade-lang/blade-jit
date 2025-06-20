package org.blade.language.runtime;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.api.utilities.TriState;
import org.blade.language.BladeLanguage;

@ExportLibrary(InteropLibrary.class)
public final class FunctionObj implements TruffleObject {

  public static final int INLINE_CACHE_SIZE = 2;

  private final TruffleString name;
  private final int argumentsCount;
  private final boolean variadic;
  private final CyclicAssumption callTargetStable;

  public RootCallTarget callTarget;

  public FunctionObj(TruffleString name, RootCallTarget target, int argumentsCount, boolean variadic) {
    this.name = name;
    this.argumentsCount = argumentsCount;
    this.variadic = variadic;
    callTarget = target;
    callTargetStable = new CyclicAssumption(name.toJavaStringUncached());
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  static int identityHashCode(FunctionObj receiver) {
    return System.identityHashCode(receiver);
  }

  public TruffleString getName() {
    return name;
  }

  public Assumption getCallTargetStable() {
    return callTargetStable.getAssumption();
  }

  public int getArgumentsCount() {
    return argumentsCount;
  }

  public boolean isVariadic() {
    return variadic;
  }

  public RootCallTarget getCallTarget() {
    return callTarget;
  }

  public void setCallTarget(RootCallTarget callTarget) {
    boolean wasNull = this.callTarget == null;
    this.callTarget = callTarget;
    if (!wasNull) {
      callTargetStable.invalidate();
    }
  }

  @ExportMessage
  boolean hasLanguage() {
    return true;
  }

  @ExportMessage
  Class<? extends TruffleLanguage<?>> getLanguage() {
    return BladeLanguage.class;
  }

  @SuppressWarnings("static-method")
  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  SourceSection getSourceLocation() {
    return callTarget.getRootNode().getEncapsulatingSourceSection();
  }

  @SuppressWarnings("static-method")
  @ExportMessage
  boolean hasSourceLocation() {
    return true;
  }

  @ExportMessage
  boolean isExecutable() {
    return true;
  }

  @ExportMessage
  boolean isNull() {
    return false;
  }

  @ExportMessage
  boolean hasMetaObject() {
    return true;
  }

  @ExportMessage
  Object getMetaObject() {
    return BladeType.FUNCTION;
  }

  @CompilerDirectives.TruffleBoundary
  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return toString();
  }

  @Override
  public String toString() {
    String format = variadic ? "<function %s(%d...) at 0x%x>" : "<function %s(%d) at 0x%x>";
    return BString.format(format, name.toJavaStringUncached(), argumentsCount, callTarget.hashCode());
  }

  @ExportMessage
  @SuppressWarnings("unused")
  static final class IsIdenticalOrUndefined {
    @Specialization
    static TriState doSLFunction(FunctionObj receiver, FunctionObj other) {
      return receiver == other ? TriState.TRUE : TriState.FALSE;
    }

    @Fallback
    static TriState doOther(FunctionObj receiver, Object other) {
      return TriState.UNDEFINED;
    }
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
    protected static Object doDirect(FunctionObj function, Object[] arguments,
                                     @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                     @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                     @Cached("create(cachedTarget)") DirectCallNode callNode) {
      return callNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(FunctionObj function, Object[] arguments,
                                       @Cached IndirectCallNode callNode) {
      return callNode.call(function.callTarget, arguments);
    }
  }
}
