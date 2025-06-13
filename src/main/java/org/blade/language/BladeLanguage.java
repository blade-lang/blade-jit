package org.blade.language;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.builtins.*;
import org.blade.language.nodes.NBlockRootNode;
import org.blade.language.nodes.expressions.NSetPropertyNodeGen;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.nodes.functions.NReadFunctionArgsExprNode;
import org.blade.language.nodes.functions.NRootFunctionNode;
import org.blade.language.nodes.literals.NSelfLiteralNode;
import org.blade.language.nodes.statements.NBlockStmtNode;
import org.blade.language.nodes.statements.NExprStmtNode;
import org.blade.language.nodes.string.NStringLiteralNode;
import org.blade.language.parser.Lexer;
import org.blade.language.parser.Parser;
import org.blade.language.parser.ast.Stmt;
import org.blade.language.runtime.*;
import org.blade.language.shared.BuiltinClassesModel;
import org.blade.language.shared.ErrorsModel;
import org.blade.language.translator.BladeTranslator;
import org.graalvm.options.*;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@TruffleLanguage.Registration(
  id = BladeLanguage.ID,
  name = "Blade",
  defaultMimeType = BladeLanguage.MIME_TYPE,
  characterMimeTypes = BladeLanguage.MIME_TYPE,
  contextPolicy = TruffleLanguage.ContextPolicy.SHARED,
  fileTypeDetectors = BladeFileDetector.class
)
@ProvidedTags({
  StatementTag.class, CallTag.class, RootTag.class, RootBodyTag.class, ExpressionTag.class, TryBlockTag.class,
  ReadVariableTag.class, WriteVariableTag.class
})
@Bind.DefaultExpression("get($node)")
public class BladeLanguage extends TruffleLanguage<BladeContext> {

  public static final String ID = "blade";
  public static final String MIME_TYPE = "application/x-blade-lang";
  public final static TruffleString.Encoding ENCODING = TruffleString.Encoding.UTF_8;
  private static final LanguageReference<BladeLanguage> REFERENCE = LanguageReference.create(BladeLanguage.class);
  private final Assumption assumption = Truffle.getRuntime().createAssumption("Single Blade context.");

  @Option(help = "Enforce type hints in function parameters", category = OptionCategory.USER, stability = OptionStability.STABLE)
  //
  public static final OptionKey<Boolean> EnforceTypes = new OptionKey<>(false);
  public boolean enforceTypes = false;

  // Shapes
  public final Shape rootShape = Shape.newBuilder().build();
  public final Shape listShape = createShape(ListObject.class);
  public final Shape dictionayShape = createShape(DictionaryObject.class);
  // models
  private final BObject objectClass = new BObject(rootShape);
  private final BladeClass functionClass = new BladeClass(rootShape, "Function", objectClass);
  public final BuiltinClassesModel builtinObjects = createBuiltinClasses();

  public static BladeLanguage get(Node node) {
    return REFERENCE.get(node);
  }

  private Shape createShape(Class<? extends BladeObject> layout) {
    return Shape.newBuilder()
      .allowImplicitCastIntToLong(true)
      .layout(layout, MethodHandles.lookup())
      .build();
  }

  @Override
  protected BladeContext createContext(Env env) {
    enforceTypes = EnforceTypes.getValue(env.getOptions());

    DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();
    return new BladeContext(
      env,
      createGlobalScope(objectLibrary),
      builtinObjects,
      new FunctionObject(
        rootShape,
        functionClass,
        "<>",
        new NBlockRootNode(
          this,
          FrameDescriptor.newBuilder().build(),
          new NBlockStmtNode(Collections.emptyList()),
          "@new"
        ).getCallTarget(),
        0
      )
    );
  }

  private BuiltinClassesModel createBuiltinClasses() {
    return new BuiltinClassesModel(
      rootShape,
      listShape,
      dictionayShape,
      objectClass,
      functionClass,
      createErrorsModel()
    );
  }

  private ErrorsModel createErrorsModel() {
    return new ErrorsModel(
      new BladeClass(rootShape, "Error", objectClass, true),
      new BladeClass(rootShape, "TypeError", objectClass, true),
      new BladeClass(rootShape, "ArgumentError", objectClass, true),
      new BladeClass(rootShape, "ValueError", objectClass, true),
      new BladeClass(rootShape, "AssertError", objectClass, true)
    );
  }

  private DynamicObject createGlobalScope(DynamicObjectLibrary objectLibrary) {


    GlobalScopeObject globalScope = new GlobalScopeObject(rootShape);

    // register built-in functions
    registerBuiltinFunctions(objectLibrary, BuiltinFunctions.class, globalScope);

    // register builtin classes and their methods
    objectLibrary.putConstant(globalScope, "Object", objectClass, 0);
    registerBuiltinMethods(objectLibrary, ObjectMethods.class, objectClass);

    objectLibrary.putConstant(globalScope, "Dictionary", builtinObjects.dictionaryObject, 0);
    registerBuiltinMethods(objectLibrary, DictionaryMethods.class, builtinObjects.dictionaryObject);

    objectLibrary.putConstant(globalScope, "List", builtinObjects.listObject, 0);
    registerBuiltinMethods(objectLibrary, ListMethods.class, builtinObjects.listObject);

    objectLibrary.putConstant(globalScope, "String", builtinObjects.stringObject, 0);
    registerBuiltinMethods(objectLibrary, StringMethods.class, builtinObjects.stringObject);

    objectLibrary.putConstant(globalScope, "Range", builtinObjects.rangeObject, 0);
    registerBuiltinMethods(objectLibrary, RangeMethods.class, builtinObjects.rangeObject);

    objectLibrary.putConstant(globalScope, "BigInt", builtinObjects.bigIntObject, 0);
    objectLibrary.putConstant(globalScope, "Number", builtinObjects.numberObject, 0);
    objectLibrary.putConstant(globalScope, "Bool", builtinObjects.booleanObject, 0);

    // add all built-in class prototypes to the global scope
    for (Map.Entry<String, BladeClass> entry : builtinObjects.builtinClasses.entrySet()) {
      objectLibrary.putConstant(globalScope, entry.getKey(), entry.getValue(), 0);
    }

    // add a constructor to all Error types
    for (Map.Entry<String, BladeClass> entry : builtinObjects.errorsModel.ALL.entrySet()) {
      objectLibrary.putConstant(
        entry.getValue(), "@new",
        // error subtype constructor
        new FunctionObject(
          rootShape,
          functionClass,
          entry.getKey(),
          new NBlockRootNode(
            this,
            FrameDescriptor.newBuilder().build(),
            new NBlockStmtNode(List.of(
              // this.message = args[1];
              new NExprStmtNode(NSetPropertyNodeGen.create(
                new NSelfLiteralNode(),
                new NReadFunctionArgsExprNode(1, "arg"),
                "message"
              )),
              // this.type = <type>;
              new NExprStmtNode(NSetPropertyNodeGen.create(
                new NSelfLiteralNode(),
                new NStringLiteralNode(entry.getKey()),
                "type"
              ))
            )),
            "@new"
          ).getCallTarget(),
          1
        ),
        0
      );
    }

    return globalScope;
  }

  private void registerBuiltinFunctions(DynamicObjectLibrary objectLibrary, Class<? extends BaseBuiltinDeclaration> source, DynamicObject scope) {
    BuiltinDeclarationAccessor.get(source).forEach((factory) -> {
      defineBuiltinFunction(objectLibrary, scope, factory.key(), factory.value(), factory.regulator());
    });
  }

  private void registerBuiltinMethods(DynamicObjectLibrary objectLibrary, Class<? extends BaseBuiltinDeclaration> source, BladeClass klass) {
    BuiltinDeclarationAccessor.get(source).forEach((factory) -> {
      defineBuiltinMethod(objectLibrary, klass, factory.key(), factory.value());
    });
  }

  private void defineBuiltinFunction(
    DynamicObjectLibrary objectLibrary, DynamicObject scope, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory, boolean variadic
  ) {
    objectLibrary.putConstant(
      scope,
      name,
      new FunctionObject(
        rootShape,
        functionClass,
        name,
        createCallTarget(factory, true),
        factory.getExecutionSignature().size(),
        variadic
      ),
      0
    );
  }

  private void defineBuiltinFunction(
    DynamicObjectLibrary objectLibrary, GlobalScopeObject globalScope, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory
  ) {
    defineBuiltinFunction(objectLibrary, globalScope, name, factory, false);
  }

  private void defineBuiltinMethod(
    DynamicObjectLibrary objectLibrary, BladeClass classObject, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory
  ) {
    objectLibrary.putConstant(
      classObject,
      name,
      new FunctionObject(
        rootShape,
        functionClass,
        name,
        createCallTarget(factory, false),
        factory.getExecutionSignature().size() - 1
      ),
      0
    );
  }

  private CallTarget createCallTarget(NodeFactory<? extends NBuiltinFunctionNode> factory, boolean offset) {
    int argumentCount = factory.getExecutionSignature().size();

    NReadFunctionArgsExprNode[] arguments = IntStream.range(0, argumentCount)
      .mapToObj(i -> new NReadFunctionArgsExprNode(offset ? i + 1 : i, "arg" + i))
      .toArray(NReadFunctionArgsExprNode[]::new);

    NRootFunctionNode rootNode = new NRootFunctionNode(this, factory.createNode((Object) arguments));

    return rootNode.getCallTarget();
  }

  @Override
  protected CallTarget parse(ParsingRequest request) {
    Source source = request.getSource();

    Parser parser = new Parser(new Lexer(source), this);
    List<Stmt> statements = parser.parse();

    var visitor = new BladeTranslator(parser, builtinObjects);
    var parseResult = visitor.translate(statements);
    return new NBlockRootNode(
      this, parseResult.frameDescriptor, parseResult.node,
      "@.script", visitor.getRootSourceSection()
    ).getCallTarget();
  }

  @Override
  protected Object getScope(BladeContext context) {
    return context.globalScope;
  }

  @Override
  protected boolean patchContext(BladeContext context, Env newEnv) {
    context.patchContext(newEnv);
    return true;
  }

  @Override
  protected void initializeMultipleContexts() {
    assumption.invalidate();
  }

  @Override
  protected boolean isVisible(BladeContext context, Object value) {
    return !InteropLibrary.getFactory().getUncached(value).isNull(value);
  }

  @Override
  public void exitContext(BladeContext context, ExitMode exitMode, int exitCode) {
    // Shutdown hooks should always be run irrespective of the exit code.
    context.runShutdownHooks();
  }

  @Override
  protected boolean areOptionsCompatible(OptionValues firstOptions, OptionValues newOptions) {
    return EnforceTypes.getValue(firstOptions).equals(EnforceTypes.getValue(newOptions));
  }

  @Override
  protected OptionDescriptors getOptionDescriptors() {
    return new BladeLanguageOptionDescriptors();
  }
}
