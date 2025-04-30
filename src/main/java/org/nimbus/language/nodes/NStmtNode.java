package org.nimbus.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import org.nimbus.language.debug.NBlockScopeDebugObject;
import org.nimbus.language.debug.NFunctionScopeDebugObject;
import org.nimbus.language.nodes.functions.NFunctionBodyNode;
import org.nimbus.language.nodes.statements.NBlockStmtNode;

@GenerateWrapper
@ExportLibrary(NodeLibrary.class)
public abstract class NStmtNode extends NNode implements InstrumentableNode {

  public final Node getParentBlock() {
    Node parent = getParent();
    while(parent != null) {
      if(parent instanceof NBlockStmtNode || parent instanceof NFunctionBodyNode) {
        break;
      }

      Node grandParent = parent.getParent();
      if(grandParent == null) {
        break;
      }

      parent = grandParent;
    }
    return parent;
  }

  /*@ExportMessage
  boolean hasScope(Frame frame,
                   @Cached(value = "this.getParentBlock()", adopt = false, allowUncached = true, neverDefault = true)
                   @Cached.Shared("parentBlock") Node parentBlock) {
    return !(parentBlock instanceof NBlockRootNode);
  }

  @ExportMessage
  Object getScope(Frame frame, boolean nodeEnter,
                  @Cached(value = "this.getParentBlock()", adopt = false, allowUncached = true, neverDefault = true)
                  @Cached.Shared("parentBlock") Node parentBlock) {
    return  parentBlock instanceof NBlockStmtNode
      ? new NBlockScopeDebugObject(frame, (NBlockStmtNode) parentBlock)
      : new NFunctionScopeDebugObject(frame, (NFunctionBodyNode) parentBlock);
  }*/

  @Override
  public boolean isInstrumentable() {
    return true;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.StatementTag.class;
  }

  @Override
  public WrapperNode createWrapper(ProbeNode probe) {
    return new NStmtNodeWrapper(this, probe);
  }
}
