package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class NReturnException extends ControlFlowException {
  public final Object value;

  public NReturnException(Object value) {
    this.value = value;
  }
}
