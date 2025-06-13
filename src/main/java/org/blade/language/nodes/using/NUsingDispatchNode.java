package org.blade.language.nodes.using;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BigIntObject;
import org.blade.language.runtime.BladeNil;

@NodeChild(value = "valueNode", type = NNode.class)
public abstract class NUsingDispatchNode extends NStmtNode {

  @Children
  private final NWhenNode[] caseNodes;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode defaultNode;

  public NUsingDispatchNode(NWhenNode[] caseNodes, NNode defaultNode) {
    this.caseNodes = caseNodes;
    this.defaultNode = defaultNode;
  }

  @Specialization
  @ExplodeLoop // Important for optimizing fixed number of cases
  protected Object doBoolean(VirtualFrame frame, boolean switchValue) {
    for (NWhenNode caseNode : caseNodes) {
      if (caseNode.execute(frame) instanceof Boolean value && switchValue == value) {
        return caseNode.bodyNode.execute(frame);
      }
    }

    // No match, execute a default case if present
    if (defaultNode != null) {
      return defaultNode.execute(frame);
    }

    return BladeNil.SINGLETON;
  }

  @Specialization
  @ExplodeLoop // Important for optimizing fixed number of cases
  protected Object doLong(VirtualFrame frame, long switchValue) {
    for (NWhenNode caseNode : caseNodes) {
      if (caseNode.execute(frame) instanceof Long value && switchValue == value) {
        return caseNode.bodyNode.execute(frame);
      }
    }

    // No match, execute a default case if present
    if (defaultNode != null) {
      return defaultNode.execute(frame);
    }

    return BladeNil.SINGLETON;
  }

  @Specialization
  @ExplodeLoop // Important for optimizing fixed number of cases
  protected Object doDouble(VirtualFrame frame, double switchValue) {
    for (NWhenNode caseNode : caseNodes) {
      if (caseNode.execute(frame) instanceof Double value && switchValue == value) {
        return caseNode.bodyNode.execute(frame);
      }
    }

    // No match, execute a default case if present
    if (defaultNode != null) {
      return defaultNode.execute(frame);
    }

    return BladeNil.SINGLETON;
  }

  @Specialization
  @ExplodeLoop // Important for optimizing fixed number of cases
  protected Object doBigInt(VirtualFrame frame, BigIntObject switchValue) {
    for (NWhenNode caseNode : caseNodes) {
      if (caseNode.execute(frame) instanceof BigIntObject value && switchValue.equals(value)) {
        return caseNode.bodyNode.execute(frame);
      }
    }

    // No match, execute a default case if present
    if (defaultNode != null) {
      return defaultNode.execute(frame);
    }

    return BladeNil.SINGLETON;
  }

  @Specialization
  @ExplodeLoop // Important for optimizing fixed number of cases
  protected Object doString(VirtualFrame frame, TruffleString switchValue,
                            @Cached TruffleString.EqualNode equalNode) {
    for (NWhenNode caseNode : caseNodes) {
      if(caseNode.execute(frame) instanceof TruffleString string) {
        if (BString.equals(switchValue, string, equalNode)) {
          return caseNode.bodyNode.execute(frame);
        }
      }
    }

    // No match, execute a default case if present
    if (defaultNode != null) {
      return defaultNode.execute(frame);
    }

    return BladeNil.SINGLETON;
  }

  @Fallback
  protected Object doOthers(VirtualFrame frame, Object switchValue,
                            @Cached InlinedConditionProfile nilProfile) {

    if (!nilProfile.profile(this, switchValue == null)) {
      for (NWhenNode caseNode : caseNodes) {
        Object caseValue = caseNode.execute(frame);

        if (!nilProfile.profile(this, caseValue == null)) {
          if(equals(caseValue, switchValue)) {
            return caseNode.bodyNode.execute(frame);
          }
        }
      }
    }

    // No match, execute a default case if present
    if (defaultNode != null) {
      return defaultNode.execute(frame);
    }

    return BladeNil.SINGLETON;
  }

  @CompilerDirectives.TruffleBoundary
  protected boolean equals(Object a, Object b) {
    return a.equals(b);
  }
}
