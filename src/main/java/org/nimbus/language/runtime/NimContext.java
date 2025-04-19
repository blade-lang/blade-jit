package org.nimbus.language.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.shared.NBuiltinClassesModel;

@Bind.DefaultExpression("get($node)")
public class NimContext {
  private static final TruffleLanguage.ContextReference<NimContext> REFERENCE = TruffleLanguage.ContextReference.create(NimbusLanguage.class);

  public final DynamicObject globalScope;
  public final NBuiltinClassesModel objectsModel;

  public NimContext(DynamicObject globalScope, NBuiltinClassesModel objectsModel) {
    this.globalScope = globalScope;
    this.objectsModel = objectsModel;
  }

  public static NimContext get(Node node) {
    return REFERENCE.get(node);
  }
}
