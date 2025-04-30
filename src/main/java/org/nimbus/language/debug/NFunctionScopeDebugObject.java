package org.nimbus.language.debug;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.nimbus.language.nodes.functions.NFunctionBodyNode;

@ExportLibrary(InteropLibrary.class)
public final class NFunctionScopeDebugObject extends NDebugObject {
  private final NFunctionBodyNode node;

  public NFunctionScopeDebugObject(Frame frame, NFunctionBodyNode parentBlock) {
    super(frame);
    this.node = parentBlock;
  }

  @Override
  protected NRefObject[] getRefs() {
    return node.getArgAndLocalVarRefs();
  }

  @ExportMessage
  Object toDisplayString(boolean allowSideEffects) {
    return node.getRootNode().getName();
  }
}
