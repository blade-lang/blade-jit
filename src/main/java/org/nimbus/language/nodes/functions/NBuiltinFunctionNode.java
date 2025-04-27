package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import org.nimbus.language.nodes.NNode;

@NodeChild(value = "arguments", type = NReadFunctionArgsExprNode[].class)
@GenerateNodeFactory
public abstract class NBuiltinFunctionNode extends NNode {

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return tag == StandardTags.ReadVariableTag.class;
  }
}
