package org.blade.language;

import com.oracle.truffle.api.dsl.NodeFactory;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.utility.RegulatedMap;

public class BuiltinDeclarationAccessor {
  public static RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> get(Class<? extends BaseBuiltinDeclaration> klass) {
    try {
      var g = klass.getConstructors()[0].newInstance();
      return ((BaseBuiltinDeclaration)g).getDeclarations();
    } catch (Exception e) {
      return new RegulatedMap<>();
    }
  }
}
