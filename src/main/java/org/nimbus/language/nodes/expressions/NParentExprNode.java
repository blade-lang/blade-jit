package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.literals.NSelfLiteralNode;
import org.nimbus.language.runtime.NimClass;
import org.nimbus.language.runtime.NimRuntimeError;

public final class NParentExprNode extends NNode {
  private final NimClass nimClass;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NSelfLiteralNode self = new NSelfLiteralNode();

  @Child
  private InteropLibrary interopLibrary;

  public NParentExprNode(NimClass nimClass) {
    this.nimClass = nimClass;
  }

  @Override
  public Object evaluateReceiver(VirtualFrame frame) {
    return execute(frame);
  }

  @Override
  public Object evaluateFunction(VirtualFrame frame, Object receiver) {
    if(interopLibrary == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      interopLibrary = insert(InteropLibrary.getFactory().createDispatched(1));
    }

    try {
      return interopLibrary.readMember(nimClass.classObject, "@new");
    } catch (UnsupportedMessageException e) {
      throw new NimRuntimeError(e.getMessage());
    } catch (UnknownIdentifierException e) {
      return languageContext().emptyFunction;
    }
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return self.execute(frame);
  }

  public Object getParentClass() {
    return this.nimClass.classObject;
  }
}
