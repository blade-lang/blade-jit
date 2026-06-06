package org.zuri.language;

import com.oracle.truffle.api.dsl.NodeFactory;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.utility.RegulatedMap;

public interface BaseBuiltinDeclaration {
  RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations();
}
