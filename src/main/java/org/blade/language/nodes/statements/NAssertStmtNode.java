package org.blade.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BladeNil;

public final class NAssertStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode assertion;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode message;

  private final CountingConditionProfile profile = CountingConditionProfile.create();

  public NAssertStmtNode(NNode assertion, NNode message) {
    this.assertion = assertion;
    this.message = message;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if (!profile.profile(assertion.executeBoolean(frame))) {
      return message.execute(frame);
    }

    return BladeNil.SINGLETON;
  }
}
