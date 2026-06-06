package org.zuri.language.nodes.functions;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.zuri.language.nodes.NNode;
import org.zuri.language.runtime.ZuriClass;
import org.zuri.language.runtime.ModuleObject;

import java.util.List;

public final class NMethodCallExprNode extends NNode {

  @Children
  private final NNode[] arguments;
  private final CountingConditionProfile branchProfile = CountingConditionProfile.create();
  private final int length;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode target;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NMethodDispatchNode dispatchNode;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NFunctionCallExprNode functionCallNode;

  public NMethodCallExprNode(NNode target, List<NNode> arguments) {
    this.target = target;
    this.arguments = arguments.toArray(new NNode[0]);
    dispatchNode = NMethodDispatchNodeGen.create();
    functionCallNode = NFunctionCallExprNodeGen.create(target, arguments);
    length = arguments.size();
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    Object receiver = target.evaluateReceiver(frame);
    Object function = target.evaluateFunction(frame, receiver);

    CompilerAsserts.compilationConstant(length);

    Object[] values = new Object[length];
    for (int i = 0; i < length; i++) {
      values[i] = arguments[i].execute(frame);
    }

    if (branchProfile.profile(
      receiver instanceof ModuleObject
        || (receiver instanceof ZuriClass zuriClass && zuriClass.isBuiltin))
    ) {
      return functionCallNode.execute(frame);
    }

    return dispatchNode.executeDispatch(function, receiver, values);
  }
}
