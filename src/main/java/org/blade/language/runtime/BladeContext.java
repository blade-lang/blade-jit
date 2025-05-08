package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.blade.language.BladeLanguage;
import org.blade.language.shared.BuiltinClassesModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@Bind.DefaultExpression("get($node)")
public class BladeContext {
  private static final TruffleLanguage.ContextReference<BladeContext> REFERENCE = TruffleLanguage.ContextReference.create(BladeLanguage.class);

  private final List<FunctionObject> shutdownHooks = new ArrayList<>();

  public final DynamicObject globalScope;
  public final BuiltinClassesModel objectsModel;
  public final FunctionObject emptyFunction;

  public final BufferedReader input;
  public final PrintWriter output;
  public final PrintWriter error;

  public TruffleLanguage.Env env;

  public BladeContext(TruffleLanguage.Env env, DynamicObject globalScope, BuiltinClassesModel objectsModel, FunctionObject emptyFunction) {
    this.globalScope = globalScope;
    this.objectsModel = objectsModel;
    this.emptyFunction = emptyFunction;
    this.env = env;

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

  public void patchContext(TruffleLanguage.Env newEnv) {
    env = newEnv;
  }

  /**
   * Register a function as a shutdown hook. Only no-parameter functions are supported.
   *
   * @param func no-parameter function to be registered as a shutdown hook
   */
  @CompilerDirectives.TruffleBoundary
  public void registerShutdownHook(FunctionObject func) {
    shutdownHooks.add(func);
  }

  /**
   * Run registered shutdown hooks. This method is designed to be executed in
   * {@link BladeLanguage#exitContext(BladeContext, TruffleLanguage.ExitMode, int)}.
   */
  public void runShutdownHooks() {
    InteropLibrary interopLibrary = InteropLibrary.getUncached();
    for (FunctionObject shutdownHook : shutdownHooks) {
      try {
        interopLibrary.execute(shutdownHook);
      } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
        throw shouldNotReachHere("Shutdown hook is not executable!", e);
      }
    }
  }
}
