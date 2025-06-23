package org.blade.language.runtime;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.strings.TruffleString;

public final class BoundFunctionObj extends FunctionObj {
  private Object instance;

  public BoundFunctionObj(TruffleString name, BladeObject instance, RootCallTarget target, int argumentsCount, boolean variadic) {
    super(name, target, argumentsCount, variadic);
    this.instance = instance;
  }

  public void setInstance(Object instance) {
    this.instance = instance;
  }

  public Object getInstance() {
    return instance;
  }

  @Override
  public String toString() {
    String format = "<instance method %s(%d" +(variadic ? "..." : "")+ ") at 0x%x>";
    return BString.format(format, name.toJavaStringUncached(), argumentsCount - 1, callTarget.hashCode());
  }
}
