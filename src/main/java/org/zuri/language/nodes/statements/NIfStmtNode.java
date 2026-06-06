package org.zuri.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.NStmtNode;
import org.zuri.language.runtime.ZuriNil;

public final class NIfStmtNode extends NStmtNode {
  private final CountingConditionProfile profile = CountingConditionProfile.create();
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode condition;
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode thenBranch;
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode elseBranch;

  public NIfStmtNode(NNode condition, NNode thenBranch, NNode elseBranch) {
    this.condition = condition;
    this.thenBranch = thenBranch;
    this.elseBranch = elseBranch;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if (profile.profile(condition.executeBoolean(frame))) {
      return thenBranch.execute(frame);
    } else if (elseBranch != null) {
      return elseBranch.execute(frame);
    }

    return ZuriNil.SINGLETON;
  }
}
