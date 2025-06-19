package org.blade.language.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.bytecode.BytecodeRootNodes;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import org.blade.language.BladeLanguage;
import org.blade.language.debug.LocalVarNodeVisitor;
import org.blade.language.debug.RefObject;
import org.blade.language.nodes.functions.NFunctionBodyNode;
import org.blade.language.runtime.BladeNil;

import java.util.List;

public final class NRootBlockStmtNode extends RootNode {

  public final RootCallTarget[] nodes;

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  private RefObject[] refCache;

  public NRootBlockStmtNode(BladeLanguage language, List<RootCallTarget> nodes) {
    super(language);
    this.nodes = nodes.toArray(new RootCallTarget[0]);
  }

  @ExplodeLoop
  @Override
  public Object execute(VirtualFrame frame) {
    int preLength = nodes.length - 1;
    for (int i = 0; i < preLength; i++) {
      nodes[i].call(frame.getArguments());
    }

    return preLength < 0 ? BladeNil.SINGLETON : nodes[preLength].call(frame.getArguments());
  }
}
