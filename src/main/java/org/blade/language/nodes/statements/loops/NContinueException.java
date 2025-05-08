package org.blade.language.nodes.statements.loops;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class NContinueException extends ControlFlowException {
  public static final NContinueException SINGLETON = new NContinueException();
}
