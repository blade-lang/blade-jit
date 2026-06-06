package org.zuri.language.nodes.literals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.zuri.language.nodes.NNode;
import org.zuri.language.runtime.BigIntObject;

import java.math.BigInteger;

public final class NBigIntLiteralNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final BigIntObject value;

  public NBigIntLiteralNode(BigInteger value) {
    this.value = new BigIntObject(value);
  }

  @Override
  public BigIntObject execute(VirtualFrame frame) {
    return value;
  }
}
