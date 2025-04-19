package org.nimbus.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.annotations.ObjectName;
import org.nimbus.language.nodes.NimTypesGen;
import org.nimbus.language.nodes.functions.NFunctionDispatchNode;
import org.nimbus.language.nodes.functions.NFunctionDispatchNodeGen;

@ExportLibrary(InteropLibrary.class)
public final class NFunctionObject extends NClassInstance {
  private final NFunctionDispatchNode dispatchNode;

  public final String name;
  public final Object methodTarget;
  public CallTarget callTarget;
  public final int argumentsCount;

  public NFunctionObject(Shape shape, NClassObject classObject,  String name, CallTarget target, int argumentsCount) {
    this(shape, classObject, name, target, argumentsCount, null);
  }

  public NFunctionObject(Shape shape, NClassObject classObject,  String name, CallTarget target, int argumentsCount, Object object) {
    super(shape, classObject);
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

    return dispatchNode.executeDispatch(this, NimNil.SINGLETON, arguments);
  }

  private boolean isRemValue(Object value) {
    return NimTypesGen.isImplicitDouble(value) ||
      NimTypesGen.isBoolean(value) ||
      value == NimNil.SINGLETON ||
      value instanceof String ||
      value instanceof TruffleString ||
      value instanceof NClassObject ||
      value instanceof NClassInstance;
  }
}
