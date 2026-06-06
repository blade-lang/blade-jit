package org.zuri.language.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.literals.NSelfLiteralNode;
import org.zuri.language.runtime.ZuriClass;
import org.zuri.language.runtime.ZuriRuntimeError;

public final class NParentExprNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final ZuriClass zuriClass;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NSelfLiteralNode self = new NSelfLiteralNode();

  @Child
  private InteropLibrary interopLibrary;

  public NParentExprNode(ZuriClass zuriClass) {
    this.zuriClass = zuriClass;
  }

  @Override
  public Object evaluateReceiver(VirtualFrame frame) {
    return execute(frame);
  }

  @Override
  public Object evaluateFunction(VirtualFrame frame, Object receiver) {
    if (interopLibrary == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      interopLibrary = insert(InteropLibrary.getFactory().createDispatched(1));
    }

    try {
      return interopLibrary.readMember(zuriClass.classObject, "@new");
    } catch (UnsupportedMessageException e) {
      throw ZuriRuntimeError.error(this, e.getMessage());
    } catch (UnknownIdentifierException e) {
      return languageContext().emptyFunction;
    }
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return self.execute(frame);
  }

  public Object getParentClass() {
    return this.zuriClass.classObject;
  }
}
