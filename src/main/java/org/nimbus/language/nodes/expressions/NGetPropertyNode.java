package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NSharedPropertyReaderNode;
import org.nimbus.language.nodes.NSharedPropertyReaderNodeGen;
import org.nimbus.language.nodes.NSharedPropertyWriterNode;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

@NodeChild("targetExpr")
@NodeField(name = "name", type = String.class)
public abstract class NGetPropertyNode extends NNode {
  public abstract NNode getTargetExpr();
  protected abstract String getName();

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NSharedPropertyReaderNode propertyReader = NSharedPropertyReaderNodeGen.create();

  @Specialization
  protected Object readProperty(Object target) {
    return propertyReader.executeRead(target, getName());
  }

  @Override
  public Object evaluateFunction(VirtualFrame frame, Object receiver) {
    return readProperty(receiver);
  }

  @Override
  public Object evaluateReceiver(VirtualFrame frame) {
    return getTargetExpr().execute(frame);
  }
}
