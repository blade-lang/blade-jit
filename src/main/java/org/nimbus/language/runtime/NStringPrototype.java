package org.nimbus.language.runtime;

import com.oracle.truffle.api.CallTarget;

public class NStringPrototype {
  public final CallTarget upperMethod;

  public NStringPrototype(CallTarget upperMethod) {
    this.upperMethod = upperMethod;
  }
}
