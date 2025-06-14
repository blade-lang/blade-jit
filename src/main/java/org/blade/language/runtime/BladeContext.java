package org.blade.language.runtime;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.functions.NRootFunctionNode;
import org.blade.language.shared.BuiltinClassesModel;

import java.io.*;
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

  public ModuleObject loadModule(Node node, String name, String path) {
    // Check cache first
    ModuleObject module = loadedModules.get(path);
    if (module != null) {
      return module;
    }

    // Parse and execute the module
    Source source = null;
    try {
      source = Source.newBuilder(BladeLanguage.ID, env.getPublicTruffleFile(path)).build();
    } catch (IOException e) {
      throw BladeRuntimeError.error(node, "Failed to load module file ", path);
    }

    DynamicObject previousGlobalScope = globalScope;
    globalScope = module = new ModuleObject(objectsModel.rootShape, path, name);

    CallTarget moduleCallTarget = parse(source);

    moduleCallTarget.call();

    loadedModules.put(path, module);
    globalScope = previousGlobalScope;
    return module;
  }

  public void invalidateModule(String modulePath) {
    CyclicAssumption assumption = language.getModuleContentAssumption(modulePath);
    if (assumption != null) {
      assumption.invalidate("Module content changed: " + modulePath);
    }

    clearModuleFromCache(modulePath);
  }

  public void clearModuleFromCache(String modulePath) {
    loadedModules.remove(modulePath);
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
