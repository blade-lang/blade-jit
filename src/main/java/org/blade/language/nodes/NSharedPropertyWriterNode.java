package org.blade.language.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.blade.language.runtime.BladeRuntimeError;

@SuppressWarnings("truffle-inlining")
public abstract class NSharedPropertyWriterNode extends NBaseNode {
  public abstract Object executeWrite(Object target, Object name, Object value);

  @Specialization(guards = "interopLibrary.isMemberWritable(target, name)", limit = "3")
  protected Object doWrite(Object target, String name, Object value,
                           @CachedLibrary("target") InteropLibrary interopLibrary) {
    try {
      interopLibrary.writeMember(target, name, value);
    } catch (UnsupportedMessageException | UnsupportedTypeException | UnknownIdentifierException e) {
      throw BladeRuntimeError.error(this, e.getMessage());
    }
    return value;
  }

  @Specialization(guards = "interopLibrary.isNull(target)", limit = "3")
  protected Object doWriteNil(Object target, Object name, Object value,
                              @CachedLibrary("target") InteropLibrary interopLibrary) {
    throw BladeRuntimeError.error(this, "Cannot set properties of nil (setting '", name, "')");
  }

  @Fallback
  protected Object doUnknown(Object target, Object name, Object value) {
    throw BladeRuntimeError.error(this, "Object of type cannot hold properties");
  }
}
