package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.nimbus.language.nodes.NNode;

import java.util.List;

public final class NMethodCallExprNode extends NNode {

  @SuppressWarnings("FieldMayBeFinal")
  @Child protected NNode target;

  @Children
  protected final NNode[] arguments;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NMethodDispatchNode dispatchNode;

  public NMethodCallExprNode(NNode target, List<NNode> arguments) {
    this.target = target;
    this.arguments = arguments.toArray(new NNode[0]);
    dispatchNode = NMethodDispatchNodeGen.create();
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    Object receiver = target.evaluateReceiver(frame);
    Object function = target.evaluateFunction(frame, receiver);

    CompilerAsserts.compilationConstant(arguments.length);

    Object[] values = new Object[arguments.length];
    for(int i = 0; i < arguments.length; i++) {
      values[i] = arguments[i].execute(frame);
    }

    return dispatchNode.executeDispatch(function, receiver, values);
  }
}
