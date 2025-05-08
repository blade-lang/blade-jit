package org.blade.language.nodes.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BString;

public final class NStringLiteralNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final TruffleString value;

  public NStringLiteralNode(String value) {
    this.value = BString.fromJavaString(value);
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
