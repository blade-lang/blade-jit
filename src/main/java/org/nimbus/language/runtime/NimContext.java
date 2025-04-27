package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
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
  public final PrintWriter error;

  public NimContext(TruffleLanguage.Env env, DynamicObject globalScope, NBuiltinClassesModel objectsModel, NFunctionObject emptyFunction) {
    this.globalScope = globalScope;
    this.objectsModel = objectsModel;
    this.emptyFunction = emptyFunction;

    input = new BufferedReader(new InputStreamReader(env.in()));
    output = new PrintWriter(env.out(), true);
    error = new PrintWriter(env.err(), true);
  }

  public static NimContext get(Node node) {
    return REFERENCE.get(node);
  }

  @CompilerDirectives.TruffleBoundary
  public void print(Object object) {
    output.print(object);
  }

  @CompilerDirectives.TruffleBoundary
  public void println(Object object) {
    output.println(object);
  }

  @CompilerDirectives.TruffleBoundary
  public void flushOutput() {
    output.flush();
  }
}
