package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.nodes.NUnaryNode;
import org.blade.language.nodes.functions.NMethodDispatchNode;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.FunctionObject;

@OperationProxy.Proxyable(allowUncached = true)
public abstract class NBitNotNode extends NUnaryNode {

  @Child
  @SuppressWarnings("FieldMayBeFinal")
  private static NMethodDispatchNode dispatchNode = NMethodDispatchNodeGen.create();

  @Specialization
  protected static long doLong(long value) {
    return ~(int) value;
  }

  @Specialization
  @CompilerDirectives.TruffleBoundary
  public static BigIntObject doBigInt(BigIntObject left) {
    return new BigIntObject(left.get().not());
  }

  @Specialization(replaces = {"doLong", "doBigInt"})
  protected static long doDouble(double value) {
    return ~(int) value;
  }

  @Specialization(limit = "3")
  protected static Object doObject(BladeObject value,
                                   @Bind Node node,
                                   @CachedLibrary("value") InteropLibrary interopLibrary) {
    Object overrideFunction = null;
    try {
      overrideFunction = interopLibrary.readMember(value, "~");
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.error(node, e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if (overrideFunction instanceof FunctionObject function) {
      return dispatchNode.executeDispatch(function, value, new Object[0]);
    }

    return doUnsupported(value, node);
  }

  @Fallback
  protected static double doUnsupported(Object left, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node, "~", left);
  }
}
