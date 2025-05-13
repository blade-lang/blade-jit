package org.blade.language.nodes.functions;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeClass;

import java.util.List;

public final class NMethodCallExprNode extends NNode {

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode target;

  @Children
  private final NNode[] arguments;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NMethodDispatchNode dispatchNode;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NFunctionDispatchNode functionDispatchNode;

  private final CountingConditionProfile branchProfile = CountingConditionProfile.create();

  public NMethodCallExprNode(NNode target, List<NNode> arguments) {
    this.target = target;
    this.arguments = arguments.toArray(new NNode[0]);
    dispatchNode = NMethodDispatchNodeGen.create();
    functionDispatchNode = NFunctionDispatchNodeGen.create();
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

    if(branchProfile.profile(receiver instanceof BladeClass)) {
      return functionDispatchNode.executeDispatch(function, values);
    }

    return dispatchNode.executeDispatch(function, receiver, values);
  }
}
