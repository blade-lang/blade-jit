package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import org.blade.language.nodes.NBinaryNode;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;

public abstract class NBitUnsignedRightShiftNode extends NBinaryNode {

  @Specialization
  protected long doLongs(long left, long right) {
    return toUInt32(left) >>> (toUInt32(right) & 31);
  }

  @Specialization(replaces = "doLongs")
  protected long doDoubles(double left, double right) {
    return toUInt32(left) >>> (toUInt32(right) & 31);
  }

  @Specialization(limit = "3")
  protected Object doObjects(BladeObject left, BladeObject right, @CachedLibrary("left") InteropLibrary interopLibrary) {
    Object overrideValue = methodOverride(">>>", left, right, interopLibrary);
    if(overrideValue != null) {
      return overrideValue;
    }

    return doUnsupported(left, right);
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw BladeRuntimeError.argumentError(this,">>>", left, right);
  }

  private int toUInt32(long value) {
    return ((int) value & Integer.MIN_VALUE);
  }

  private int toUInt32(double value) {
    return ((int) value & Integer.MIN_VALUE);
  }
}
