package org.blade.language;

import com.oracle.truffle.api.dsl.NodeFactory;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.utility.RegulatedMap;

public interface BaseBuiltinDeclaration {
  RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations();
}
