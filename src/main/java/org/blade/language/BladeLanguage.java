package org.blade.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.builtins.*;
import org.blade.language.builtins.ListMethods;
import org.blade.language.builtins.ObjectMethods;
import org.blade.language.nodes.NBlockRootNode;
import org.blade.language.nodes.expressions.NSetPropertyNodeGen;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.nodes.functions.NFunctionRootNode;
import org.blade.language.nodes.functions.NReadFunctionArgsExprNode;
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
  // Shapes
  public final Shape rootShape = Shape.newBuilder().build();
  public final Shape listShape = createShape(ListObject.class);
  // models
  private final BObject objectClass = new BObject(rootShape);
  private final BladeClass functionClass = new BladeClass(rootShape, "Function", objectClass);
  public final BuiltinClassesModel builtinObjects = createBuiltinClasses();

  public static BladeLanguage get(Node node) {
    return REFERENCE.get(node);
  }

  private Shape createShape(Class<? extends BladeObject> layout) {
    return Shape.newBuilder()
//      .allowImplicitCastIntToLong(true)
      .layout(layout, MethodHandles.lookup())
      .build();
  }

  @Override
  protected BladeContext createContext(Env env) {
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
        0)
    );
  }

  private BuiltinClassesModel createBuiltinClasses() {
    return new BuiltinClassesModel(
      rootShape,
      listShape,
      objectClass,
      functionClass,
      new BladeClass(rootShape, "List", objectClass),
      new BladeClass(rootShape, "String", objectClass),
      createErrorsModel()
    );
  }

  private ErrorsModel createErrorsModel() {
    return new ErrorsModel(
      new BladeClass(rootShape, "Error", objectClass),
      new BladeClass(rootShape, "TypeError", objectClass),
      new BladeClass(rootShape, "ArgumentError", objectClass),
      new BladeClass(rootShape, "ValueError", objectClass)
    );
  }

  private DynamicObject createGlobalScope(DynamicObjectLibrary objectLibrary) {


    GlobalScopeObject globalScope = new GlobalScopeObject(rootShape);

    // built-in functions
    BuiltinDeclarationAccessor.get(BuiltinFunctions.class).forEach((factory) -> {
      defineBuiltinFunction(objectLibrary, globalScope, factory.key(), factory.value(), factory.regulator());
    });

    // Object class
    BuiltinDeclarationAccessor.get(ObjectMethods.class).forEach((factory) -> {
      defineBuiltinMethod(objectLibrary, objectClass, factory.key(), factory.value());
    });

    // List class
    BuiltinDeclarationAccessor.get(ListMethods.class).forEach((factory) -> {
      defineBuiltinMethod(objectLibrary, builtinObjects.listObject, factory.key(), factory.value());
    });

    // String class
    BuiltinDeclarationAccessor.get(StringMethods.class).forEach((factory) -> {
      defineBuiltinMethod(objectLibrary, builtinObjects.stringObject, factory.key(), factory.value());
    });

    // global classes
    objectLibrary.putConstant(globalScope, "Object", objectClass, 0);
    objectLibrary.putConstant(globalScope, "String", builtinObjects.stringObject, 0);
    objectLibrary.putConstant(globalScope, "List", builtinObjects.listObject, 0);

    // add all built-in class prototypes to the global scope
    for (Map.Entry<String, BladeClass> entry : builtinObjects.builtinClasses.entrySet()) {
      objectLibrary.putConstant(globalScope, entry.getKey(), entry.getValue(), 0);
    }

    // add a constructor to all Error types
    for (Map.Entry<String, BladeClass> entry : builtinObjects.errorsModel.ALL.entrySet()) {
      objectLibrary.putConstant(entry.getValue(), "@new",
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
          1),
        0);
    }

    return globalScope;
  }

  private void defineBuiltinFunction(
    DynamicObjectLibrary objectLibrary, GlobalScopeObject globalScope, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory, boolean variadic
  ) {
    objectLibrary.putConstant(
      globalScope,
      name,
      new FunctionObject(rootShape, functionClass, name, createCallTarget(factory, true), factory.getExecutionSignature().size(), variadic),
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
      new FunctionObject(rootShape, functionClass, name, createCallTarget(factory, false), factory.getExecutionSignature().size() - 1),
      0
    );
  }

  private CallTarget createCallTarget(NodeFactory<? extends NBuiltinFunctionNode> factory, boolean offset) {
    int argumentCount = factory.getExecutionSignature().size();

    NReadFunctionArgsExprNode[] arguments = IntStream.range(0, argumentCount)
      .mapToObj(i -> new NReadFunctionArgsExprNode(offset ? i + 1 : i, "arg"+i))
      .toArray(NReadFunctionArgsExprNode[]::new);

    NFunctionRootNode rootNode = new NFunctionRootNode(this, factory.createNode((Object) arguments));

    return rootNode.getCallTarget();
  }

  @Override
  protected CallTarget parse(ParsingRequest request) throws Exception {
    Source source = request.getSource();

    Parser parser = new Parser(new Lexer(source));
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
}
