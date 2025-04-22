package org.nimbus.language.nodes.statements.loops;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class NBreakException extends ControlFlowException {
  public static final NBreakException SINGLETON = new NBreakException();
}
