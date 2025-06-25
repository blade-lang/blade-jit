package org.blade.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.BlockNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.blade.language.debug.LocalVarNodeVisitor;
import org.blade.language.debug.RefObject;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.nodes.functions.NFunctionBodyNode;
import org.blade.language.runtime.BladeNil;

import java.util.List;

public final class NBlockStmtNode extends NStmtNode implements BlockNode.ElementExecutor<NNode> {

  @Child private BlockNode<NNode> block;

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  private RefObject[] refCache;

  private final boolean isProgram;

  public NBlockStmtNode(List<NNode> nodes) {
    this(nodes, false);
  }

  public NBlockStmtNode(List<NNode> nodes, boolean isProgram) {
    this.block = !nodes.isEmpty() ? BlockNode.create(nodes.toArray(new NNode[0]), this) : null;
    this.isProgram = isProgram;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    if (this.block != null) {
      this.block.executeVoid(frame, BlockNode.NO_ARGUMENT);
    }

    return BladeNil.SINGLETON;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return isProgram && tag == StandardTags.RootTag.class;
  }

  public RefObject[] getLocalVarRefs() {
    if (refCache == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      refCache = this.findLocalVarRefs();
    }
    return refCache;
  }

  private RefObject[] findLocalVarRefs() {
    LocalVarNodeVisitor visitor = new LocalVarNodeVisitor();
    NodeUtil.forEachChild(this, visitor);
    RefObject[] variables = visitor.refs.toArray(new RefObject[0]);

    Node parentBlock = getParentBlock();
    RefObject[] parentVars = parentBlock instanceof NBlockStmtNode block
      ? block.getLocalVarRefs()
      : (
      parentBlock instanceof NFunctionBodyNode function
        ? function.getArgAndLocalVarRefs()
        : null
    );

    if (parentVars == null || parentVars.length == 0) {
      return variables;
    }

    RefObject[] allVariables = new RefObject[variables.length + parentVars.length];
    System.arraycopy(variables, 0, allVariables, 0, variables.length);
    System.arraycopy(parentVars, 0, allVariables, variables.length, parentVars.length);

    return allVariables;
  }

  @Override
  public void executeVoid(VirtualFrame frame, NNode node, int index, int argument) {
    node.execute(frame);
  }
}
