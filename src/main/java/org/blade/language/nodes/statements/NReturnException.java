package org.blade.language.nodes.statements;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class NReturnException extends ControlFlowException {
  public final Object value;

  public NReturnException(Object value) {
    this.value = value;
  }
}
