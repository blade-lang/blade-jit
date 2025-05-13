package org.blade.language.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.blade.language.nodes.functions.NFunctionBodyNode;

@ExportLibrary(InteropLibrary.class)
public final class FunctionScopeDebugObject extends DebugObject {
  private final NFunctionBodyNode node;

  public FunctionScopeDebugObject(Frame frame, NFunctionBodyNode parentBlock) {
    super(frame);
    this.node = parentBlock;
  }

  @Override
  protected RefObject[] getRefs() {
    return node.getArgAndLocalVarRefs();
  }

  @CompilerDirectives.TruffleBoundary
  @ExportMessage
  Object toDisplayString(boolean allowSideEffects) {
    return node.getRootNode().getName();
  }
}
