package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;
import org.nimbus.language.runtime.NimNil;

import java.util.List;

public final class NBlockStmtNode extends NStmtNode {

  @Children
  public final NNode[] nodes;

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
}
