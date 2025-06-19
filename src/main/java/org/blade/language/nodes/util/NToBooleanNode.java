package org.blade.language.nodes.util;

import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeRuntimeError;

@NodeChild
@OperationProxy.Proxyable(allowUncached = true)
public abstract class NToBooleanNode extends NNode {
  public abstract boolean executeBoolean(VirtualFrame vrame);

  @Specialization
  public static boolean doBoolean(boolean value) {
    return value;
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
    throw BladeRuntimeError.error(node, "Cannot convert ", value, " to boolean");
  }
}
