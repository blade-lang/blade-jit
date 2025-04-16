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

  public NimContext(Shape shape, NStringPrototype stringPrototype) {
    globalScope = new NGlobalScopeObject(shape);
    this.stringPrototype = stringPrototype;
  }

  public static NimContext get(Node node) {
    return REFERENCE.get(node);
  }
}
