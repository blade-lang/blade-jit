package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.nimbus.language.debug.NLocalVarNodeVisitor;
import org.nimbus.language.debug.NRefObject;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;
import org.nimbus.language.nodes.functions.NFunctionBodyNode;
import org.nimbus.language.runtime.NimNil;

import java.util.List;

public final class NBlockStmtNode extends NStmtNode {

  @Children
  public final NNode[] nodes;

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  private NRefObject[] refCache;

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
    /*Object result = NimNil.SINGLETON;
    for(NNode node : nodes) {
      result = node.execute(frame);
    }
    return result;*/

    int preLength = nodes.length - 1;
    for(int i = 0; i < preLength; i++) {
      nodes[i].execute(frame);
    }

    return preLength < 0 ? NimNil.SINGLETON : nodes[preLength].execute(frame);
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return isProgram && tag == StandardTags.RootTag.class;
  }

  public NRefObject[] getLocalVarRefs() {
    if (refCache == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      refCache = this.findLocalVarRefs();
    }
    return refCache;
  }

  private NRefObject[] findLocalVarRefs() {
    NLocalVarNodeVisitor visitor = new NLocalVarNodeVisitor();
    NodeUtil.forEachChild(this, visitor);
    NRefObject[] variables = visitor.refs.toArray(new NRefObject[0]);

    Node parentBlock = getParentBlock();
    NRefObject[] parentVars = parentBlock instanceof NBlockStmtNode block
      ? block.getLocalVarRefs()
      : (
        parentBlock instanceof NFunctionBodyNode function
          ? function.getArgAndLocalVarRefs()
          : null
    );

    if (parentVars == null || parentVars.length == 0) {
      return variables;
    }

    NRefObject[] allVariables = new NRefObject[variables.length + parentVars.length];
    System.arraycopy(variables, 0, allVariables, 0, variables.length);
    System.arraycopy(parentVars, 0, allVariables, variables.length, parentVars.length);

    return allVariables;
  }
}
