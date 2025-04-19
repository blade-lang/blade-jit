package org.nimbus.language.nodes.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NString;

public final class NStringLiteralNode extends NNode {
  private final TruffleString value;

  public NStringLiteralNode(String value) {
    this.value = NString.fromJavaString(value);
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return !value.isEmpty();
  }

  @Override
  public TruffleString execute(VirtualFrame frame) {
    return value;
  }
}
