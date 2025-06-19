package org.blade.language.nodes.util;

import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.BladeTypes;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.*;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@TypeSystemReference(BladeTypes.class)
@NodeChild
@OperationProxy.Proxyable(allowUncached = true)
public abstract class NUnboxNode extends NNode {
  public static final int LIMIT = 5;

  @Specialization
  public static TruffleString fromString(String value,
                                         @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
    return BString.fromObject(fromJavaStringNode, value);
  }

  @Specialization
  public static TruffleString fromTruffleString(TruffleString value) {
    return value;
  }

  @Specialization
  public static boolean fromBoolean(boolean value) {
    return value;
  }

  @Specialization
  public static long fromLong(long value) {
    return value;
  }

  @Specialization
  public static double fromDouble(double value) {
    return value;
  }

  @Specialization
  public static BigIntObject fromBigInt(BigIntObject value) {
    return value;
  }

  @Specialization
  public static FunctionObject fromFunction(FunctionObject value) {
    return value;
  }

  @Specialization
  public static ListObject fromList(ListObject value) {
    return value;
  }

  @Specialization
  public static DictionaryObject fromDictionary(DictionaryObject value) {
    return value;
  }

  @Specialization
  public static RangeObject fromRange(RangeObject value) {
    return value;
  }

  @Specialization
  public static BladeClass fromClass(BladeClass value) {
    return value;
  }

  @Specialization
  public static ModuleObject fromModule(ModuleObject value) {
    return value;
  }

  @Specialization
  public static BladeNil fromFunction(BladeNil value) {
    return value;
  }

  @Specialization(limit = "LIMIT")
  public static Object fromForeign(Object value, @CachedLibrary("value") InteropLibrary interop) {
    try {
      if (interop.fitsInLong(value)) {
        return interop.asLong(value);
      } else if (interop.fitsInBigInteger(value)) {
        return value;
      } else if (interop.fitsInDouble(value)) {
        return interop.asDouble(value);
      } else if (interop.isString(value)) {
        return interop.asTruffleString(value);
      } else if (interop.isBoolean(value)) {
        return interop.asBoolean(value);
      } else {
        return value;
      }
    } catch (UnsupportedMessageException e) {
      throw shouldNotReachHere(e);
    }
  }
}
