package org.nimbus.language.debug;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.nimbus.language.nodes.NBlockRootNode;
import org.nimbus.language.nodes.functions.NFunctionBodyNode;
import org.nimbus.language.nodes.statements.NBlockStmtNode;

@ExportLibrary(InteropLibrary.class)
public final class NBlockScopeDebugObject extends NDebugObject {
  final NBlockStmtNode node;

  public NBlockScopeDebugObject(Frame frame, NBlockStmtNode parentBlock) {
    super(frame);
    this.node = parentBlock;
  }

  @Override
  protected NRefObject[] getRefs() {
    return node.getLocalVarRefs();
  }

  @ExportMessage
  Object toDisplayString(boolean allowSideEffects,
                         @Cached(value = "this.node.getParentBlock()", adopt = false, allowUncached = true, neverDefault = true)
                         @Cached.Shared("grandParent")
                         Node grandParent) {
    return grandParent instanceof RootNode grand
      ? grand.getName()
      : "block";
  }

  @ExportMessage
  boolean hasScopeParent(@Cached(value = "this.node.getParentBlock()", adopt = false, allowUncached = true, neverDefault = true)
                         @Cached.Shared("grandParent") Node grandParent) {
    return !(grandParent instanceof NBlockRootNode);
  }

  @ExportMessage
  Object getScopeParent(@Cached(value = "this.node.getParentBlock()", adopt = false, allowUncached = true, neverDefault = true)
                        @Cached.Shared("grandParent") Node grandParent) throws UnsupportedMessageException {
    if (grandParent instanceof NBlockStmtNode grand) {
      return new NBlockScopeDebugObject(frame, grand);
    } else if (grandParent instanceof NFunctionBodyNode grand) {
      return new NFunctionScopeDebugObject(frame, grand);
    } else {
      throw UnsupportedMessageException.create();
    }
  }
}
