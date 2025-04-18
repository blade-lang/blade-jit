package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.nimbus.language.nodes.NNode;

public final class NConditionalNode extends NNode {
  @CompilerDirectives.CompilationFinal
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode condition;

  @CompilerDirectives.CompilationFinal
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode leftNode;

  @CompilerDirectives.CompilationFinal
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode rightNode;

  private final CountingConditionProfile profile = CountingConditionProfile.create();

  public NConditionalNode(NNode condition, NNode leftNode, NNode rightNode) {
    this.condition = condition;
    this.leftNode = leftNode;
    this.rightNode = rightNode;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if(profile.profile(condition.executeBoolean(frame))) {
      return leftNode.execute(frame);
    }
    return rightNode.execute(frame);
  }
}
