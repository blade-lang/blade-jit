package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.literals.NSelfLiteralNode;
import org.blade.language.runtime.BladeClass;
import org.blade.language.runtime.BladeRuntimeError;

public final class NParentExprNode extends NNode {
  @CompilerDirectives.CompilationFinal
  private final BladeClass bladeClass;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NSelfLiteralNode self = new NSelfLiteralNode();

  @Child
  private InteropLibrary interopLibrary;

  public NParentExprNode(BladeClass bladeClass) {
    this.bladeClass = bladeClass;
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
      return interopLibrary.readMember(bladeClass.classObject, "@new");
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.create(e.getMessage());
    } catch (UnknownIdentifierException e) {
      return languageContext().emptyFunction;
    }
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return self.execute(frame);
  }

  public Object getParentClass() {
    return this.bladeClass.classObject;
  }
}
