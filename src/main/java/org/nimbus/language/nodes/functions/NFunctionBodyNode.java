package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.nimbus.language.debug.NFunctionArgRefObject;
import org.nimbus.language.debug.NLocalVarNodeVisitor;
import org.nimbus.language.debug.NRefObject;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;
import org.nimbus.language.nodes.statements.NBlockStmtNode;
import org.nimbus.language.nodes.statements.NReturnException;
import org.nimbus.language.runtime.NimNil;

import java.util.HashSet;
import java.util.Set;

public final class NFunctionBodyNode extends NStmtNode {
  @Children
  private final NNode[] nodes;

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  private NRefObject[] argsRefCache;

  private final BranchProfile exceptionTaken = BranchProfile.create();
  private final BranchProfile nullTaken = BranchProfile.create();

  public NFunctionBodyNode(NBlockStmtNode node) {
    this.nodes = node.nodes;
  }

  @Override
  @ExplodeLoop
  public Object execute(VirtualFrame frame) {
    for (NNode node : nodes) {
      try {
        node.execute(frame);
      } catch (NReturnException e) {
        exceptionTaken.enter();
        return e.value;
      }
    }

    nullTaken.enter();
    return NimNil.SINGLETON;
  }

  public NRefObject[] getArgAndLocalVarRefs() {
    if (argsRefCache == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      argsRefCache = findArgAndLocalVarRefs();
    }
    return argsRefCache;
  }

  @ExplodeLoop
  private NRefObject[] findArgAndLocalVarRefs() {
    Set<NFunctionArgRefObject> funcArgs = new HashSet<>();

    // The first argument is always special - it represents 'this'.
    // We'll never encounter 'this' below, because we check for ReadFunctionArgExprNode,
    // while 'this' has its own Node (ThisExprNode)
    funcArgs.add(new NFunctionArgRefObject("self", null, 0));

    NodeUtil.forEachChild(this, new NodeVisitor() {
      @Override
      public boolean visit(Node visitedNode) {
        if (visitedNode instanceof NReadFunctionArgsExprNode readNode) {
          funcArgs.add(new NFunctionArgRefObject(
            readNode.name,
            readNode.getSourceSection(),
            readNode.index));
          return true;
        }
        return NodeUtil.forEachChild(visitedNode, this);
      }
    });

    var localVarNodeVisitor = new NLocalVarNodeVisitor();
    NodeUtil.forEachChild(this, localVarNodeVisitor);

    var allReferences = new NRefObject[funcArgs.size() +
      localVarNodeVisitor.refs.size()];
    var i = 0;
    for (var funcArg : funcArgs) {
      allReferences[i++] = funcArg;
    }
    for (var localVar : localVarNodeVisitor.refs) {
      allReferences[i++] = localVar;
    }
    return allReferences;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.RootTag.class;
  }
}
