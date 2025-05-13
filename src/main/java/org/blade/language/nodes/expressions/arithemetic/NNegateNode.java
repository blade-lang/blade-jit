package org.blade.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.blade.language.nodes.NUnaryNode;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeRuntimeError;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

public abstract class NNegateNode extends NUnaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLong(long value) {
    return -value;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public BigIntObject doBigInt(BigIntObject left) {
    return new BigIntObject(left.get().negate());
  }

  @Specialization(replaces = {"doLong", "doBigInt"})
  protected double doDouble(double value) {
    return -value;
  }

  @Fallback
  protected double doUnsupported(Object value) {
    throw BladeRuntimeError.argumentError(this,"-", value);
  }
}
