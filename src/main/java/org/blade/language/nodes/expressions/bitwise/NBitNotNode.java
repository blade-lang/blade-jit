package org.blade.language.nodes.expressions.bitwise;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.blade.language.nodes.NUnaryNode;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.FunctionObject;

public abstract class NBitNotNode extends NUnaryNode {

  @Specialization
  protected long doLongs(long value) {
    return ~(int)value;
  }

  @Specialization(replaces = "doLongs")
  protected long doDoubles(double value) {
    return ~(int)value;
  }

  @Specialization(limit = "3")
  protected Object doObject(BladeObject value,
                            @CachedLibrary("value") InteropLibrary interopLibrary) {
    Object overrideFunction = null;
    try {
      overrideFunction = interopLibrary.readMember(value, "~");
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.create(e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if(overrideFunction instanceof FunctionObject function) {
      return NMethodDispatchNodeGen.create().executeDispatch(function, value, new Object[0]);
    }

    return doUnsupported(value);
  }

  @Fallback
  protected double doUnsupported(Object left) {
    throw BladeRuntimeError.argumentError(this,"~", left);
  }
}
