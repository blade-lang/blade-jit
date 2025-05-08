package org.blade.language.runtime;

import com.oracle.truffle.api.CallTarget;

public class StringPrototype {
  public final CallTarget upperMethod;
  public final CallTarget indexOfMethod;

  public StringPrototype(CallTarget upperMethod, CallTarget indexOfMethod) {
    this.upperMethod = upperMethod;
    this.indexOfMethod = indexOfMethod;
  }
}
