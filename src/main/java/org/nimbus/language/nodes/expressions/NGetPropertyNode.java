package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NSharedPropertyReaderNode;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

@NodeChild("targetExpr")
@NodeField(name = "name", type = String.class)
public abstract class NGetPropertyNode extends NNode {
  protected abstract String getName();

  @Specialization
  protected Object readProperty(Object target,
                              @Cached NSharedPropertyReaderNode propertyReaderNode) {
    return propertyReaderNode.executeRead(target, getName());
  }
}
