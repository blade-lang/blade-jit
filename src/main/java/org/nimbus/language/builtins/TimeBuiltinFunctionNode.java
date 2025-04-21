package org.nimbus.language.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;

@GenerateNodeFactory
public abstract class TimeBuiltinFunctionNode extends NBuiltinFunctionNode {
  @Specialization
  protected long doAny() {
    return time() / 1000;
  }

  @CompilerDirectives.TruffleBoundary
  private long time() {
    return System.currentTimeMillis();
  }
}
