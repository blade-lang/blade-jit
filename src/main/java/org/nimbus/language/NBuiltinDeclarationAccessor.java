package org.nimbus.language;

import com.oracle.truffle.api.dsl.NodeFactory;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.utility.RegulatedMap;

public class NBuiltinDeclarationAccessor {
  public static RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> get(Class<? extends NBaseBuiltinDeclaration> klass) {
    try {
      var g = klass.getConstructors()[0].newInstance();
      return ((NBaseBuiltinDeclaration)g).getDeclarations();
    } catch (Exception e) {
      return new RegulatedMap<>();
    }
  }
}
