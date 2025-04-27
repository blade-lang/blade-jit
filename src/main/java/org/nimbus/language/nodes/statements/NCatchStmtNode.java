package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

public class NCatchStmtNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode body;

  // NOTE: Intentionally not compilation final!
  private final int slot;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode asBody;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode thenBody;

  public NCatchStmtNode(NNode body, NNode thenBody) {
    this(body, -1, null, thenBody);
  }

  public NCatchStmtNode(NNode body, int slot, NNode asBody, NNode thenBody) {
    this.body = body;
    this.slot = slot;
    this.asBody = asBody;
    this.thenBody = thenBody;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if (this.slot == -1) {
      try {
        return body.execute(frame);
      } finally {
        if(thenBody != null) {
          thenBody.execute(frame);
        }
      }
    } else {
      try {
        return body.execute(frame);
      } catch (NimRuntimeError e) {
        frame.setObject(slot, e.value);
        if(asBody != null) {
          return asBody.execute(frame);
        }

        return NimNil.SINGLETON;
      } finally {
        if (thenBody != null) {
          thenBody.execute(frame);
        }
      }
    }
  }
}
