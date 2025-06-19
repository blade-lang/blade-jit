package org.blade.language.translator;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.bytecode.BytecodeRootNodes;
import com.oracle.truffle.api.frame.FrameDescriptor;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NBytecodeRootNode;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NRootBlockStmtNode;
import org.blade.language.nodes.statements.NBlockStmtNode;

import java.util.List;

public class NTranslateResult {
  public final NRootBlockStmtNode node;
  public final FrameDescriptor frameDescriptor;

  public NTranslateResult(BladeLanguage language, List<RootCallTarget> nodeList, FrameDescriptor frameDescriptor) {
    node = new NRootBlockStmtNode(language, nodeList);
    this.frameDescriptor = frameDescriptor;
  }
}
