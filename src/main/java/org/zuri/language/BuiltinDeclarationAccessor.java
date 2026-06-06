package org.zuri.language;

import com.oracle.truffle.api.dsl.NodeFactory;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.utility.RegulatedMap;

public class BuiltinDeclarationAccessor {
  public static RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> get(Class<? extends BaseBuiltinDeclaration> klass) {
    try {
      var g = klass.getConstructors()[0].newInstance();
      return ((BaseBuiltinDeclaration) g).getDeclarations();
    } catch (Exception e) {
      return new RegulatedMap<>();
    }
  }
}
