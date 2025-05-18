package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;

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
  protected static Object doObjects(BladeObject left, BladeObject right,
                                    @Bind Node node, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(node, ">>>", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right, node);
  }

  @Fallback
  protected static double doUnsupported(Object left, Object right, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node,">>>", left, right);
  }

  private static int toUInt32(long value) {
    return ((int) value & Integer.MIN_VALUE);
  }

  private static int toUInt32(double value) {
    return ((int) value & Integer.MIN_VALUE);
  }
}
