package org.zuri.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.zuri.language.nodes.NBinaryNode;
import org.zuri.language.runtime.ZuriObject;
import org.zuri.language.runtime.ZuriRuntimeError;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NBitUnsignedRightShiftNode extends NBinaryNode {

  @Specialization
  protected static long doLongs(long left, long right) {
    return toUInt32(left) >>> (toUInt32(right) & 31);
  }

  @Specialization(replaces = "doLongs")
  protected static long doDoubles(double left, double right) {
    return toUInt32(left) >>> (toUInt32(right) & 31);
  }

  @Specialization(limit = "3")
  protected static Object doObjects(ZuriObject left, ZuriObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, ">>>", left, right, interopLibrary);
    if (overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  protected static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw ZuriRuntimeError.argumentError(node, "operator >>>", left, right);
  }

  private static int toUInt32(long value) {
    return ((int) value & Integer.MIN_VALUE);
  }

  private static int toUInt32(double value) {
    return ((int) value & Integer.MIN_VALUE);
  }
}
