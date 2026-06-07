package org.zuri.language;

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
import org.zuri.language.builtins.*;
import org.zuri.language.nodes.NBlockRootNode;
import org.zuri.language.nodes.expressions.NSetPropertyNodeGen;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.language.nodes.functions.NReadFunctionArgsExprNode;
import org.zuri.language.nodes.functions.NRootFunctionNode;
import org.zuri.language.nodes.literals.NSelfLiteralNode;
import org.zuri.language.nodes.statements.NBlockStmtNode;
import org.zuri.language.nodes.statements.NExprStmtNode;
import org.zuri.language.nodes.string.NStringLiteralNode;
import org.zuri.language.parser.Lexer;
import org.zuri.language.parser.Parser;
import org.zuri.language.parser.ast.Stmt;
import org.zuri.language.runtime.*;
import org.zuri.language.shared.BuiltinClassesModel;
import org.zuri.language.shared.ErrorsModel;
import org.zuri.language.translator.ZuriTranslator;
import org.graalvm.options.*;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@TruffleLanguage.Registration(
  id = ZuriLanguage.ID,
  name = "Zuri",
  defaultMimeType = ZuriLanguage.MIME_TYPE,
  characterMimeTypes = ZuriLanguage.MIME_TYPE,
  contextPolicy = TruffleLanguage.ContextPolicy.SHARED,
  fileTypeDetectors = ZuriFileDetector.class,
  dependentLanguages = "regex"
)
@ProvidedTags({
  StatementTag.class, CallTag.class, RootTag.class, RootBodyTag.class, ExpressionTag.class, TryBlockTag.class,
  ReadVariableTag.class, WriteVariableTag.class
})
@Bind.DefaultExpression("get($node)")
public class ZuriLanguage extends TruffleLanguage<ZuriContext> {

  public static final String ID = "zuri";
  public static final String MIME_TYPE = "application/x-zuri-lang";
  public final static TruffleString.Encoding ENCODING = TruffleString.Encoding.UTF_8;
  @Option(help = "Enforce type hints in function parameters", category = OptionCategory.USER, stability = OptionStability.STABLE)
  //
  public static final OptionKey<Boolean> EnforceTypes = new OptionKey<>(false);
  private static final LanguageReference<ZuriLanguage> REFERENCE = LanguageReference.create(ZuriLanguage.class);
  // Shapes
  public final Shape rootShape = Shape.newBuilder().build();
  public final Shape listShape = createShape(ListObject.class);
  public final Shape dictionayShape = createShape(DictionaryObject.class);
  private final Assumption assumption = Truffle.getRuntime().createAssumption("Single Zuri context.");
  // models
  private final BObject objectClass = new BObject(rootShape);
  private final ZuriClass functionClass = new ZuriClass(rootShape, "Function", objectClass);
  public final BuiltinClassesModel builtinObjects = createBuiltinClasses();
  public boolean enforceTypes = false;

  public static ZuriLanguage get(Node node) {
    return REFERENCE.get(node);
  }

  private Shape createShape(Class<? extends ZuriObject> layout) {
    return Shape.newBuilder()
      .allowImplicitCastIntToLong(true)
      .layout(layout, MethodHandles.lookup())
      .build();
  }

  @Override
  protected ZuriContext createContext(Env env) {
    enforceTypes = EnforceTypes.getValue(env.getOptions());

    DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();
    return new ZuriContext(
      this,
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
      new ZuriClass(rootShape, "Error", objectClass, true),
      new ZuriClass(rootShape, "TypeError", objectClass, true),
      new ZuriClass(rootShape, "ArgumentError", objectClass, true),
      new ZuriClass(rootShape, "ValueError", objectClass, true),
      new ZuriClass(rootShape, "AssertError", objectClass, true)
    );
  }

  private DynamicObject createGlobalScope(DynamicObjectLibrary objectLibrary) {

    GlobalScopeObject globalScope = new GlobalScopeObject(rootShape);

    // register built-in functions
    registerBuiltinFunctions(objectLibrary, globalScope);

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
    for (Map.Entry<String, ZuriClass> entry : builtinObjects.builtinClasses.entrySet()) {
      objectLibrary.putConstant(globalScope, entry.getKey(), entry.getValue(), 0);
    }

    // add a constructor to all Error types
    for (Map.Entry<String, ZuriClass> entry : builtinObjects.errorsModel.ALL.entrySet()) {
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

  private void registerBuiltinFunctions(DynamicObjectLibrary objectLibrary, DynamicObject scope) {
    BuiltinDeclarationAccessor.get(BuiltinFunctions.class).forEach((factory) -> {
      defineBuiltinFunction(objectLibrary, scope, factory.key(), factory.value(), factory.regulator());
    });
  }

  private void registerBuiltinMethods(DynamicObjectLibrary objectLibrary, Class<? extends BaseBuiltinDeclaration> source, ZuriClass klass) {
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
        ZuriContext.createCallTarget(this, factory, true),
        factory.getExecutionSignature().size(),
        variadic
      ),
      0
    );
  }

  private void defineBuiltinMethod(
    DynamicObjectLibrary objectLibrary, ZuriClass classObject, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory
  ) {
    objectLibrary.putConstant(
      classObject,
      name,
      new FunctionObject(
        rootShape,
        functionClass,
        name,
        ZuriContext.createCallTarget(this, factory, false),
        factory.getExecutionSignature().size() - 1
      ),
      0
    );
  }

  @Override
  protected CallTarget parse(ParsingRequest request) {
    Source source = request.getSource();

    Parser parser = new Parser(new Lexer(source), this);
    List<Stmt> statements = parser.parse();

    var visitor = new ZuriTranslator(parser, builtinObjects);
    var parseResult = visitor.translate(statements);
    return new NBlockRootNode(
      this, parseResult.frameDescriptor, parseResult.node,
      "@.script", visitor.getRootSourceSection()
    ).getRootNode().getCallTarget();
  }

  @Override
  protected Object getScope(ZuriContext context) {
    return context.globalScope;
  }

  @Override
  protected boolean patchContext(ZuriContext context, Env newEnv) {
    context.patchContext(newEnv);
    return true;
  }

  @Override
  protected void initializeMultipleContexts() {
    assumption.invalidate();
  }

  @Override
  protected boolean isVisible(ZuriContext context, Object value) {
    return !InteropLibrary.getFactory().getUncached(value).isNull(value);
  }

  @Override
  public void exitContext(ZuriContext context, ExitMode exitMode, int exitCode) {
    // Shutdown hooks should always be run irrespective of the exit code.
    context.runShutdownHooks();
  }

  @Override
  protected boolean areOptionsCompatible(OptionValues firstOptions, OptionValues newOptions) {
    return EnforceTypes.getValue(firstOptions).equals(EnforceTypes.getValue(newOptions));
  }

  @Override
  protected OptionDescriptors getOptionDescriptors() {
    return new ZuriLanguageOptionDescriptors();
  }
}
