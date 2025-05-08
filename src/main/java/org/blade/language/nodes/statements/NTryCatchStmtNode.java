package org.blade.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BladeRuntimeError;

public final class NTryCatchStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode body;

  // NOTE: Intentionally not compilation final!
  private final int slot;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode catchBody;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode finallyBody;

  public NTryCatchStmtNode(NNode body, NNode finallyBody) {
    this(body, -1, null, finallyBody);
  }

  public NTryCatchStmtNode(NNode body, int slot, NNode catchBody, NNode finallyBody) {
    this.body = body;
    this.slot = slot;
    this.catchBody = catchBody;
    this.finallyBody = finallyBody;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if (this.slot == -1) {
      try {
        return body.execute(frame);
      } finally {
        finallyBody.execute(frame);
      }
    } else {
      try {
        return body.execute(frame);
      } catch (BladeRuntimeError e) {
        frame.setObject(slot, e.value);
        return catchBody.execute(frame);
      } finally {
        if (finallyBody != null) {
          finallyBody.execute(frame);
        }
      }
    }
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.TryBlockTag.class;
  }
}
