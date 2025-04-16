package org.nimbus.language.runtime;

import com.oracle.truffle.api.exception.AbstractTruffleException;

public class NimRuntimeError extends AbstractTruffleException {
  public NimRuntimeError(String message) {
    super(message);
  }
}
