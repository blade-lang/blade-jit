package org.nimbus.language.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.shared.NBuiltinClassesModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

@Bind.DefaultExpression("get($node)")
public class NimContext {
  private static final TruffleLanguage.ContextReference<NimContext> REFERENCE = TruffleLanguage.ContextReference.create(NimbusLanguage.class);

  public final DynamicObject globalScope;
  public final NBuiltinClassesModel objectsModel;
  public final NFunctionObject emptyFunction;

  public final BufferedReader input;
  public final PrintWriter output;

  public NimContext(TruffleLanguage.Env env, DynamicObject globalScope, NBuiltinClassesModel objectsModel, NFunctionObject emptyFunction) {
    this.globalScope = globalScope;
    this.objectsModel = objectsModel;
    this.emptyFunction = emptyFunction;

    this.input = new BufferedReader(new InputStreamReader(env.in()));
    this.output = new PrintWriter(env.out(), true);
  }

  public static NimContext get(Node node) {
    return REFERENCE.get(node);
  }
}
