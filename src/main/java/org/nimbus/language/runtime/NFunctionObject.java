package org.nimbus.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.nimbus.language.nodes.NimTypesGen;
import org.nimbus.language.nodes.calls.NFunctionDispatchNode;
import org.nimbus.language.nodes.calls.NFunctionDispatchNodeGen;

@ExportLibrary(InteropLibrary.class)
public final class NFunctionObject implements TruffleObject {
  private final NFunctionDispatchNode dispatchNode;

  public final String name;
  public final Object methodTarget;
  public CallTarget callTarget;
  public final int argumentsCount;

  public NFunctionObject(String name, CallTarget target, int argumentsCount) {
    this(name, target, argumentsCount, null);
  }

  public NFunctionObject(String name, CallTarget target, int argumentsCount, Object object) {
    callTarget = target;
    dispatchNode = NFunctionDispatchNodeGen.create();
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

  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return toString();
  }

  @Override
  public String toString() {
    return String.format("<function %s() at 0x%x>", name, callTarget.hashCode());
  }

  @ExportMessage
  Object execute(Object[] arguments) {
    for (Object argument : arguments) {
      if (!isRemValue(argument)) {
        throw new NimRuntimeError("invalid function argument value '" + argument + "'");
      }
    }

    return dispatchNode.executeDispatch(this, arguments);
  }

  private boolean isRemValue(Object value) {
    return NimTypesGen.isImplicitDouble(value) ||
      NimTypesGen.isBoolean(value) ||
      value == NimNil.SINGLETON ||
      value instanceof NBaseObject ||
      value instanceof NFunctionObject;
  }
}
