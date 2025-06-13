package org.blade.language.debug;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeVisitor;
import org.blade.language.nodes.statements.NBlockStmtNode;
import org.blade.language.nodes.statements.NExprStmtNode;
import org.blade.language.nodes.statements.NLocalAssignNode;

import java.util.ArrayList;
import java.util.List;

public class LocalVarNodeVisitor implements NodeVisitor {
  public final List<LocalVarRefObject> refs = new ArrayList<>(4);
  private boolean inDeclaration = false;

  @Override
  public boolean visit(Node node) {
    if (node instanceof NExprStmtNode stmtNode) {
      if (stmtNode.discardValue) {
        inDeclaration = true;
      }

      NodeUtil.forEachChild(stmtNode, this);
      inDeclaration = false;
      return true;
    }

    // Write to a variable is a declaration unless it exists already in a parent scope.
    if (inDeclaration && node instanceof NLocalAssignNode assignNode) {
      refs.add(new LocalVarRefObject(
        assignNode.getSlotName(),
        assignNode.getSourceSection(),
        assignNode.getSlot()
      ));
      return true;
    }

    // Recur into any Node except a block of statements.
    if (!(node instanceof NBlockStmtNode)) {
      NodeUtil.forEachChild(node, this);
    }

    return true;
  }
}
