package org.blade.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.nodes.NUnaryNode;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeRuntimeError;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NNegateNode extends NUnaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected static long doLong(long value) {
    return -value;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doBigInt(BigIntObject left) {
    return new BigIntObject(left.get().negate());
  }

  @Specialization(replaces = {"doLong", "doBigInt"})
  protected static double doDouble(double value) {
    return -value;
  }

  @Fallback
  protected static double doUnsupported(Object value, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node,"-", value);
  }
}
