package org.blade.language.debug;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.blade.language.nodes.NBlockRootNode;
import org.blade.language.nodes.functions.NFunctionBodyNode;
import org.blade.language.nodes.statements.NBlockStmtNode;

@ExportLibrary(InteropLibrary.class)
public final class BlockScopeDebugObject extends DebugObject {
  final NBlockStmtNode node;

  public BlockScopeDebugObject(Frame frame, NBlockStmtNode parentBlock) {
    super(frame);
    this.node = parentBlock;
  }

  @Override
  protected RefObject[] getRefs() {
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
      return new BlockScopeDebugObject(frame, grand);
    } else if (grandParent instanceof NFunctionBodyNode grand) {
      return new FunctionScopeDebugObject(frame, grand);
    } else {
      throw UnsupportedMessageException.create();
    }
  }
}
