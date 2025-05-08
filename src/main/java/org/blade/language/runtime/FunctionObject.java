package org.blade.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.BladeTypesGen;
import org.blade.language.nodes.functions.NMethodDispatchNode;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;

@ExportLibrary(InteropLibrary.class)
public final class FunctionObject extends BladeObject {
  @CompilerDirectives.CompilationFinal
  private final NMethodDispatchNode dispatchNode;

  @CompilerDirectives.CompilationFinal
  public final String name;

  @CompilerDirectives.CompilationFinal
  public CallTarget callTarget;

  @CompilerDirectives.CompilationFinal
  public final int argumentsCount;

  @CompilerDirectives.CompilationFinal
  public final boolean variadic;

  public FunctionObject(Shape shape, BladeClass classObject, String name, CallTarget target, int argumentsCount) {
    this(shape, classObject, name, target, argumentsCount, false);
  }

  public FunctionObject(Shape shape, BladeClass classObject, String name, CallTarget target, int argumentsCount, boolean variadic) {
    super(shape, classObject);
    callTarget = target;
    dispatchNode = NMethodDispatchNodeGen.create();
    this.name = name;
    this.argumentsCount = argumentsCount;
    this.variadic = variadic;
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
    return BString.format("<function %s() at 0x%x>", name, callTarget.hashCode());
  }

  @ExplodeLoop
  @ExportMessage
  Object execute(Object[] arguments) {
    for (Object argument : arguments) {
      if (!isRemValue(argument)) {
        throw BladeRuntimeError.create("invalid function argument value '", argument, "'");
      }
    }

    return dispatchNode.executeDispatch(this, BladeNil.SINGLETON, arguments);
  }

  private boolean isRemValue(Object value) {
    return BladeTypesGen.isImplicitDouble(value) ||
      BladeTypesGen.isBoolean(value) ||
      value == BladeNil.SINGLETON ||
      value instanceof String ||
      value instanceof TruffleString ||
      value instanceof BladeObject;
  }
}
