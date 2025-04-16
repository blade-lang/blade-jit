package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

@NodeChild("targetExpr")
@NodeField(name = "name", type = String.class)
public abstract class NGetPropertyNode extends NNode {
  protected abstract String getName();

  @Specialization(guards = "interopLibrary.hasMembers(target)", limit = "3")
  protected Object readExists(Object target,
                              @CachedLibrary("target") InteropLibrary interopLibrary) {
    try {
      return interopLibrary.readMember(target, getName());
    } catch (UnknownIdentifierException e) {
      return NimNil.SINGLETON;
    } catch (UnsupportedMessageException e) {
      throw new NimRuntimeError(e.getMessage());
    }
  }

  @Specialization(guards = "interopLibrary.isNull(target)", limit = "3")
  protected Object readNil(Object target,
                           @CachedLibrary("target") InteropLibrary interopLibrary) {
    throw new NimRuntimeError("Cannot read properties of undefined (reading '" + getName() + "')");
  }

  @Fallback
  protected Object readUnsupported(Object target) {
    throw new NimRuntimeError("Object of type has no named property " + getName());
  }
}
