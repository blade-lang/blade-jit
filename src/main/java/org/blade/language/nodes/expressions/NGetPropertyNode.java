package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NSharedPropertyReaderNode;
import org.blade.language.nodes.NSharedPropertyReaderNodeGen;

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
    NNode expr = getTargetExpr();

    Object target = expr instanceof NParentExprNode parentNode
      ? parentNode.getParentClass()
      : receiver;

    return readProperty(target);
  }

  @Override
  public Object evaluateReceiver(VirtualFrame frame) {
    return getTargetExpr().execute(frame);
  }
}
