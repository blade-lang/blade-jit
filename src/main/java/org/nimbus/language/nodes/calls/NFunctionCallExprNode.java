package org.nimbus.language.nodes.calls;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.nimbus.language.nodes.NNode;

import java.util.List;

public final class NFunctionCallExprNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode target;

  @Children
  private final NNode[] arguments;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NFunctionDispatchNode dispatchNode;

  public NFunctionCallExprNode(NNode target, List<NNode> arguments) {
    this.target = target;
    this.arguments = arguments.toArray(new NNode[0]);
    dispatchNode = NFunctionDispatchNodeGen.create();
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    Object function = target.execute(frame);

    CompilerAsserts.compilationConstant(arguments.length);

    Object[] values = new Object[arguments.length];
    for(int i = 0; i < arguments.length; i++) {
      values[i] = arguments[i].execute(frame);
    }

    return dispatchNode.executeDispatch(function, values);
  }
}
