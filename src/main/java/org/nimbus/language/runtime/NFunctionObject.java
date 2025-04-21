package org.nimbus.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.NimTypesGen;
import org.nimbus.language.nodes.functions.NMethodDispatchNode;
import org.nimbus.language.nodes.functions.NMethodDispatchNodeGen;

@ExportLibrary(InteropLibrary.class)
public final class NFunctionObject extends NimObject {
  private final NMethodDispatchNode dispatchNode;

  public final String name;
  public final Object methodTarget;
  public CallTarget callTarget;
  public final int argumentsCount;

  public NFunctionObject(Shape shape, NimClass classObject, String name, CallTarget target, int argumentsCount) {
    this(shape, classObject, name, target, argumentsCount, null);
  }

  public NFunctionObject(Shape shape, NimClass classObject, String name, CallTarget target, int argumentsCount, Object object) {
    super(shape, classObject);
    callTarget = target;
    dispatchNode = NMethodDispatchNodeGen.create();
    this.name = name;
    this.argumentsCount = argumentsCount;
    this.methodTarget = object;
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
    return NString.format("<function %s() at 0x%x>", name, callTarget.hashCode());
  }

  @ExplodeLoop
  @ExportMessage
  Object execute(Object[] arguments) {
    for (Object argument : arguments) {
      if (!isRemValue(argument)) {
        throw NimRuntimeError.create("invalid function argument value '", argument, "'");
      }
    }

    return dispatchNode.executeDispatch(this, NimNil.SINGLETON, arguments);
  }

  private boolean isRemValue(Object value) {
    return NimTypesGen.isImplicitDouble(value) ||
      NimTypesGen.isBoolean(value) ||
      value == NimNil.SINGLETON ||
      value instanceof String ||
      value instanceof TruffleString ||
      value instanceof NimObject;
  }
}
