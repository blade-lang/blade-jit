package org.blade.language.nodes.list;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.expressions.NParentExprNode;

@SuppressWarnings("truffle-inlining")
@NodeChild("listExpr")
@NodeChild("indexExpr")
public abstract class NListIndexReadNode extends NNode {
  protected abstract NNode getListExpr();

  protected abstract NNode getIndexExpr();

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NReadListIndexNode innerNode = NReadListIndexNode.create();

  @Specialization
  protected Object doIndexOrProperty(Object target, Object indexOrProperty) {
    return innerNode.executeRead(target, indexOrProperty);
  }

  @Override
  public Object evaluateReceiver(VirtualFrame frame) {
    return getListExpr().execute(frame);
  }

  @Override
  public Object evaluateFunction(VirtualFrame frame, Object receiver) {
    Object property = getIndexExpr().execute(frame);

    NNode expr = getListExpr();
    Object target = expr instanceof NParentExprNode parentNode
      ? parentNode.getParentClass()
      : receiver;

    return doIndexOrProperty(target, property);
  }


}
