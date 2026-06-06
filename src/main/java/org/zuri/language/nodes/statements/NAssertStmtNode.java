package org.zuri.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.NStmtNode;
import org.zuri.language.runtime.ZuriNil;

public final class NAssertStmtNode extends NStmtNode {
  private final CountingConditionProfile profile = CountingConditionProfile.create();
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode assertion;
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode message;

  public NAssertStmtNode(NNode assertion, NNode message) {
    this.assertion = assertion;
    this.message = message;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if (!profile.profile(assertion.executeBoolean(frame))) {
      return message.execute(frame);
    }

    return ZuriNil.SINGLETON;
  }
}
