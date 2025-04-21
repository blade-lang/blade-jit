package org.nimbus.language.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@GenerateNodeFactory
public abstract class MicroTimeBuiltinFunctionNode extends NBuiltinFunctionNode {
  @Specialization
  protected long doAny() {
    return microTime();
  }

  @CompilerDirectives.TruffleBoundary
  private long microTime() {
    return ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
  }
}
