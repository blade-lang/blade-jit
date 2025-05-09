package org.blade.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BladeClass;

import java.util.List;

public final class NClassDeclNode extends NStmtNode {
  @Children
  private final NNode[] methods;

  @Children
  private final NNode[] propreties;

  @Children
  private final NNode[] operators;

  @CompilerDirectives.CompilationFinal
  private final BladeClass classObject;

  public NClassDeclNode(List<NNode> methods, List<NNode> properties, List<NNode> operators, BladeClass classObject) {
    this.methods = methods.toArray(new NNode[0]);
    this.propreties = properties.toArray(new NNode[0]);
    this.operators = operators.toArray(new NNode[0]);
    this.classObject = classObject;
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    // Expand properties first as methods may need them.
    for(NNode property : propreties) {
      property.execute(frame);
    }

    // followed by operators since methods can need them too.
    for(NNode operator : operators) {
      operator.execute(frame);
    }

    // lastly, the methods
    for(NNode method : methods) {
      method.execute(frame);
    }

    return classObject;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return false;
  }

  @Override
  public SourceSection getSourceSection() {
    // we want to disable debuggers from reaching class declarations
    return null;
  }
}
