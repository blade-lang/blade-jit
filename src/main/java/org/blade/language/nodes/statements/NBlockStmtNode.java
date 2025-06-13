package org.blade.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
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

public final class NBlockStmtNode extends NStmtNode {

  @Children
  public final NNode[] nodes;

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  private RefObject[] refCache;

  private final boolean isProgram;

  public NBlockStmtNode(List<NNode> nodes) {
    this(nodes, false);
  }

  public NBlockStmtNode(List<NNode> nodes, boolean isProgram) {
    this.nodes = nodes.toArray(new NNode[0]);
    this.isProgram = isProgram;
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    /*Object result = BladeNil.SINGLETON;
    for(NNode node : nodes) {
      result = node.execute(frame);
    }
    return result;*/

    int preLength = nodes.length - 1;
    for (int i = 0; i < preLength; i++) {
      nodes[i].execute(frame);
    }

    return preLength < 0 ? BladeNil.SINGLETON : nodes[preLength].execute(frame);
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
}
