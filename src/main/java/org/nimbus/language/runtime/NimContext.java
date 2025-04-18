package org.nimbus.language.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.NimbusLanguage;

@Bind.DefaultExpression("get($node)")
public class NimContext {
  private static final TruffleLanguage.ContextReference<NimContext> REFERENCE = TruffleLanguage.ContextReference.create(NimbusLanguage.class);

  public final NGlobalScopeObject globalScope;
  public final NStringPrototype stringPrototype;
  public final Shape listShape;
  public final Shape rootShape;

  public NimContext(NimbusLanguage language) {
    globalScope = new NGlobalScopeObject(language.rootShape);
    stringPrototype = language.createStringPrototype();
    listShape = language.listShape;
    rootShape = language.rootShape;
  }

  public static NimContext get(Node node) {
    return REFERENCE.get(node);
  }
}
