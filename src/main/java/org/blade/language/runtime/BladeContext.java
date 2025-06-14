package org.blade.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.Source;
import org.blade.language.BladeLanguage;
import org.blade.language.shared.BuiltinClassesModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@Bind.DefaultExpression("get($node)")
public class BladeContext {
  private static final TruffleLanguage.ContextReference<BladeContext> REFERENCE = TruffleLanguage.ContextReference.create(
    BladeLanguage.class);

  private final List<FunctionObject> shutdownHooks = new ArrayList<>();

  public DynamicObject globalScope;
  public final BuiltinClassesModel objectsModel;
  public final FunctionObject emptyFunction;

  public final BufferedReader input;
  public final PrintWriter output;
  public final PrintWriter error;

  public TruffleLanguage.Env env;

  public final BladeLanguage language;
  private final Map<String, ModuleObject> loadedModules = new ConcurrentHashMap<>();

  public BladeContext(BladeLanguage language, TruffleLanguage.Env env, DynamicObject globalScope, BuiltinClassesModel objectsModel, FunctionObject emptyFunction) {
    this.language = language;
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

  @CompilerDirectives.TruffleBoundary
  private Source getSource(Node node, String path) {
    try {
      return Source.newBuilder(BladeLanguage.ID, env.getPublicTruffleFile(path)).build();
    } catch (IOException e) {
      throw BladeRuntimeError.error(node, "Failed to load module file ", path);
    }
  }

  public ModuleObject loadModule(Node node, String name, String path) {
    // Check cache first
    ModuleObject module = getCachedModule(path);
    if (module != null) {
      return module;
    }

    // Parse and execute the module
    Source source = getSource(node, path);

    DynamicObject previousGlobalScope = globalScope;
    globalScope = module = new ModuleObject(objectsModel.rootShape, path, name);

    CallTarget moduleCallTarget = parse(source);

    moduleCallTarget.call();

    registerModule(path, module);
    globalScope = previousGlobalScope;
    return module;
  }

  @CompilerDirectives.TruffleBoundary
  private ModuleObject getCachedModule(String path) {
    return loadedModules.get(path);
  }

  @CompilerDirectives.TruffleBoundary
  private void registerModule(String path, ModuleObject module) {
    loadedModules.put(path, module);
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

  public CallTarget parse(Source source) {
    return env.parsePublic(source);
  }

  public BladeContext duplicate() {
    return new BladeContext(
      language,
      env,
      ((GlobalScopeObject) globalScope).duplicate(DynamicObjectLibrary.getUncached()),
      objectsModel,
      emptyFunction
    );
  }

  public TruffleObject getBindings() {
    return (TruffleObject) env.getPolyglotBindings();
  }
}
