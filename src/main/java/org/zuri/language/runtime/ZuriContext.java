package org.zuri.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.BaseBuiltinDeclaration;
import org.zuri.language.ZuriLanguage;
import org.zuri.language.BuiltinDeclarationAccessor;
import org.zuri.language.builtins.std.MathStdModule;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.language.nodes.functions.NReadFunctionArgsExprNode;
import org.zuri.language.nodes.functions.NRootFunctionNode;
import org.zuri.language.shared.BuiltinClassesModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

@Bind.DefaultExpression("get($node)")
public class ZuriContext {
  private static final TruffleLanguage.ContextReference<ZuriContext> REFERENCE = TruffleLanguage.ContextReference.create(
    ZuriLanguage.class);
  public final BuiltinClassesModel objectsModel;
  public final FunctionObject emptyFunction;
  public final BufferedReader input;
  public final PrintWriter output;
  public final PrintWriter error;
  public final ZuriLanguage language;
  private final List<FunctionObject> shutdownHooks = new ArrayList<>();
  private final Map<String, ModuleObject> loadedModules = new ConcurrentHashMap<>();
  private final Map<TruffleString, ModuleObject> builtinModules = new ConcurrentHashMap<>();
  private final List<Class<? extends BaseBuiltinDeclaration>> BUILTIN_MODULES_REGISTRY = List.of(
    MathStdModule.class
  );
  public DynamicObject globalScope;
  public TruffleLanguage.Env env;
  private final RegexCache regexCache;

  public ZuriContext(ZuriLanguage language, TruffleLanguage.Env env, DynamicObject globalScope, BuiltinClassesModel objectsModel, FunctionObject emptyFunction) {
    this.language = language;
    this.globalScope = globalScope;
    this.objectsModel = objectsModel;
    this.emptyFunction = emptyFunction;
    this.env = env;

    input = new BufferedReader(new InputStreamReader(env.in()));
    output = new PrintWriter(env.out(), true);
    error = new PrintWriter(env.err(), true);

    regexCache = new RegexCache(env);

    createBuiltinModules();
  }

  public static ZuriContext get(Node node) {
    return REFERENCE.get(node);
  }

  public static RegexCache getCache(Node node) {
    return REFERENCE.get(node).regexCache;
  }

  public RegexCache getRegexCache() {
    return regexCache;
  }

  public static RootCallTarget createCallTarget(ZuriLanguage language, NodeFactory<? extends NBuiltinFunctionNode> factory, boolean offset) {
    int argumentCount = factory.getExecutionSignature().size();

    NReadFunctionArgsExprNode[] arguments = IntStream.range(0, argumentCount)
      .mapToObj(i -> new NReadFunctionArgsExprNode(offset ? i + 1 : i, "arg" + i))
      .toArray(NReadFunctionArgsExprNode[]::new);

    NRootFunctionNode rootNode = new NRootFunctionNode(language, factory.createNode((Object) arguments));

    return rootNode.getRootNode().getCallTarget();
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
      return Source.newBuilder(ZuriLanguage.ID, env.getPublicTruffleFile(path)).build();
    } catch (IOException e) {
      throw ZuriRuntimeError.error(node, "Failed to load module file ", path);
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
   * {@link ZuriLanguage#exitContext(ZuriContext, TruffleLanguage.ExitMode, int)}.
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

  public ZuriContext duplicate() {
    return new ZuriContext(
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

  private void createBuiltinModules() {
    final DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();

    BUILTIN_MODULES_REGISTRY.forEach(m -> {
      String moduleName = "_" + Arrays.stream(m.getName().split("[.]"))
        .toList()
        .getLast()
        .toLowerCase(Locale.ROOT)
        .replaceAll("stdmodule$", "");

      var module = new ModuleObject(objectsModel.rootShape, ZString.concatString("<native-module ", moduleName, ">"), moduleName);

      BuiltinDeclarationAccessor.get(m).forEach((factory) -> {
        objectLibrary.putConstant(
          module,
          factory.key(),
          new FunctionObject(
            objectsModel.rootShape,
            objectsModel.functionObject,
            factory.key(),
            createCallTarget(language, factory.value(), true),
            factory.value().getExecutionSignature().size(),
            factory.regulator()
          ),
          0
        );
      });

      builtinModules.putIfAbsent(ZString.fromJavaString(moduleName), module);
    });
  }

  @CompilerDirectives.TruffleBoundary
  public ModuleObject getBuiltinModule(TruffleString name) {
    return builtinModules.getOrDefault(name, null);
  }
}
