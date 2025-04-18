package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NClassObject;

import java.util.List;

public final class NClassDeclNode extends NNode {
  @Children
  private final NNode[] methods;

  private final NClassObject classObject;

  public NClassDeclNode(List<NNode> methods, NClassObject classObject) {
    this.methods = methods.toArray(new NNode[0]);
    this.classObject = classObject;
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    for(NNode method : methods) {
      method.execute(frame);
    }

    return classObject;
  }
}
