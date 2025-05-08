package org.blade.language.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.runtime.BladeNil;

import java.util.List;

public class NModuleNode extends NNode {

  @Children
  private final NNode[] nodes;

  public NModuleNode(List<NNode> nodes) {
    this.nodes = nodes.toArray(new NNode[0]);
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    int preLength = nodes.length - 1;
    for(int i = 0; i < preLength; i++) {
      nodes[i].execute(frame);
    }

    return preLength < 0 ? BladeNil.SINGLETON : nodes[preLength].execute(frame);
  }
}
