package org.blade.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BladeNil;

public final class NIfStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode condition;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode thenBranch;

  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode elseBranch;

  private final CountingConditionProfile profile = CountingConditionProfile.create();

  public NIfStmtNode(NNode condition, NNode thenBranch, NNode elseBranch) {
    this.condition = condition;
    this.thenBranch = thenBranch;
    this.elseBranch = elseBranch;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if(profile.profile(condition.executeBoolean(frame))) {
      return thenBranch.execute(frame);
    } else if(elseBranch != null) {
      return elseBranch.execute(frame);
    }

    return BladeNil.SINGLETON;
  }
}
