package org.blade.language.nodes.util;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.string.NReadStringPropertyNode;
import org.blade.language.nodes.string.NReadStringPropertyNodeGen;
import org.blade.language.runtime.*;

import java.math.BigInteger;

@NodeChild
@OperationProxy.Proxyable(allowUncached = true)
public abstract class NToBooleanNode extends NNode {

  public static NToBooleanNode getUncached() {
    return NToBooleanNodeGen.getUncached();
  }

  @NeverDefault
  public static NToBooleanNode create() {
    return NToBooleanNodeGen.create();
  }

  public abstract boolean executeBoolean(Object object);

  @Specialization
  public static boolean doBoolean(boolean value) {
    return value;
  }

  @Specialization
  public static boolean doLong(long value) {
    return value > 0;
  }

  @Specialization
  public static boolean doDouble(double value) {
    return value > 0;
  }

  @Specialization
  public static boolean doString(TruffleString value, @Cached TruffleString.CodePointLengthNode lengthNode) {
    return BString.length(value, lengthNode) > 0;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static boolean doString(BigIntObject value) {
    return value.get().compareTo(BigInteger.ZERO) > 0;
  }

  @Specialization
  public static boolean doString(ListObject value) {
    return value.items.length > 0;
  }

  @Specialization
  public static boolean doString(RangeObject value) {
    return value.upper != value.lower;
  }

  @Specialization(guards = "lib.isBoolean(value)", limit = "3")
  public static boolean doInterop(Object value,
                                  @Bind Node node,
                                  @CachedLibrary("value") InteropLibrary lib) {
    try {
      return lib.asBoolean(value);
    } catch (UnsupportedMessageException e) {
      return doFallback(value, node);
    }
  }

  @Fallback
  public static boolean doFallback(Object value, @Bind Node node) {
    return true;
  }
}
