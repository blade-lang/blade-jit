package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.blade.language.BladeLanguage;
import org.blade.language.shared.BuiltinClassesModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

@Bind.DefaultExpression("get($node)")
public class BladeContext {
  private static final TruffleLanguage.ContextReference<BladeContext> REFERENCE = TruffleLanguage.ContextReference.create(BladeLanguage.class);

  public final DynamicObject globalScope;
  public final BuiltinClassesModel objectsModel;
  public final FunctionObject emptyFunction;

  public final BufferedReader input;
  public final PrintWriter output;
  public final PrintWriter error;

  public BladeContext(TruffleLanguage.Env env, DynamicObject globalScope, BuiltinClassesModel objectsModel, FunctionObject emptyFunction) {
    this.globalScope = globalScope;
    this.objectsModel = objectsModel;
    this.emptyFunction = emptyFunction;

    input = new BufferedReader(new InputStreamReader(env.in()));
    output = new PrintWriter(env.out(), true);
    error = new PrintWriter(env.err(), true);
  }

  public static BladeContext get(Node node) {
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
