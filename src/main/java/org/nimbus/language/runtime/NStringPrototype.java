package org.nimbus.language.runtime;

import com.oracle.truffle.api.CallTarget;

public class NStringPrototype {
  public final CallTarget upperMethod;
  public final CallTarget indexOfMethod;

  public NStringPrototype(CallTarget upperMethod, CallTarget indexOfMethod) {
    this.upperMethod = upperMethod;
    this.indexOfMethod = indexOfMethod;
  }
}
