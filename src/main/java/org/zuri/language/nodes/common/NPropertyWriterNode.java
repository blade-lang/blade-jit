package org.zuri.language.nodes.common;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.zuri.language.nodes.NBaseNode;
import org.zuri.language.runtime.ZuriRuntimeError;

@SuppressWarnings("truffle-inlining")
public abstract class NPropertyWriterNode extends NBaseNode {
  public abstract Object executeWrite(Object target, Object name, Object value);

  @Specialization(guards = "interopLibrary.isMemberWritable(target, name)", limit = "3")
  protected static Object doWrite(Object target, String name, Object value, @Bind Node node,
                           @CachedLibrary("target") InteropLibrary interopLibrary) {
    try {
      interopLibrary.writeMember(target, name, value);
    } catch (UnsupportedMessageException | UnsupportedTypeException | UnknownIdentifierException e) {
      throw ZuriRuntimeError.error(node, e.getMessage());
    }
    return value;
  }

  @Specialization(guards = "interopLibrary.isNull(target)", limit = "3")
  protected static Object doWriteNil(Object target, Object name, Object value, @Bind Node node,
                              @CachedLibrary("target") InteropLibrary interopLibrary) {
    throw ZuriRuntimeError.error(node, "Cannot set properties of nil (setting '", name, "')");
  }

  @Fallback
  protected static Object doUnknown(Object target, Object name, Object value, @Bind Node node) {
    throw ZuriRuntimeError.error(node, "Object of type cannot hold properties");
  }
}
