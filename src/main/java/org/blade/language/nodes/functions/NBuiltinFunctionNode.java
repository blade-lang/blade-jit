package org.blade.language.nodes.functions;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import org.blade.language.nodes.NNode;

@NodeChild(value = "arguments", type = NReadFunctionArgsExprNode[].class)
@GenerateNodeFactory
public abstract class NBuiltinFunctionNode extends NNode {
}
