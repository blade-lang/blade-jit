package org.nimbus.language;

import com.oracle.truffle.api.dsl.NodeFactory;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.utility.RegulatedMap;

public interface NBaseBuiltinDeclaration {
  RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations();
}
