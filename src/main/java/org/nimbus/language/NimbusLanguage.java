package org.nimbus.language;

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
import org.nimbus.language.builtins.NBuiltinFunctions;
import org.nimbus.language.builtins.NListMethods;
import org.nimbus.language.builtins.NObjectMethods;
import org.nimbus.language.builtins.NStringMethods;
import org.nimbus.language.nodes.NBlockRootNode;
import org.nimbus.language.nodes.expressions.NSetPropertyNodeGen;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.nodes.functions.NFunctionRootNode;
import org.nimbus.language.nodes.functions.NReadFunctionArgsExprNode;
import org.nimbus.language.nodes.literals.NSelfLiteralNode;
import org.nimbus.language.nodes.statements.NBlockStmtNode;
import org.nimbus.language.nodes.statements.NExprStmtNode;
import org.nimbus.language.nodes.string.NStringLiteralNode;
import org.nimbus.language.parser.Lexer;
import org.nimbus.language.parser.Parser;
import org.nimbus.language.parser.ast.Stmt;
import org.nimbus.language.runtime.*;
import org.nimbus.language.shared.NBuiltinClassesModel;
import org.nimbus.language.shared.NErrorsModel;
import org.nimbus.language.translator.NimTranslator;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@TruffleLanguage.Registration(
  id = NimbusLanguage.ID,
  name = "Nimbus",
  defaultMimeType = NimbusLanguage.MIME_TYPE,
  characterMimeTypes = NimbusLanguage.MIME_TYPE,
  contextPolicy = TruffleLanguage.ContextPolicy.SHARED,
  fileTypeDetectors = NimFileDetector.class
)
@ProvidedTags({
  StatementTag.class, CallTag.class, RootTag.class, RootBodyTag.class, ExpressionTag.class, TryBlockTag.class,
  ReadVariableTag.class, WriteVariableTag.class
})
@Bind.DefaultExpression("get($node)")
public class NimbusLanguage extends TruffleLanguage<NimContext> {

  public static final String ID = "nim";
  public static final String MIME_TYPE = "application/x-nimbus-lang";
  public final static TruffleString.Encoding ENCODING = TruffleString.Encoding.UTF_8;
  private static final LanguageReference<NimbusLanguage> REFERENCE = LanguageReference.create(NimbusLanguage.class);
  // Shapes
  public final Shape rootShape = Shape.newBuilder().build();
  public final Shape listShape = createShape(NListObject.class);
  // models
  private final NObject objectClass = new NObject(rootShape);
  private final NimClass functionClass = new NimClass(rootShape, "Function", objectClass);
  public final NBuiltinClassesModel builtinObjects = createBuiltinClasses();

  public static NimbusLanguage get(Node node) {
    return REFERENCE.get(node);
  }

  private Shape createShape(Class<? extends NimObject> layout) {
    return Shape.newBuilder()
      .allowImplicitCastIntToLong(true)
      .layout(layout, MethodHandles.lookup())
      .build();
  }

  @Override
  protected NimContext createContext(Env env) {
    DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();
    return new NimContext(
      env,
      createGlobalScope(objectLibrary),
      builtinObjects,
      new NFunctionObject(
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

  private NBuiltinClassesModel createBuiltinClasses() {
    return new NBuiltinClassesModel(
      rootShape,
      listShape,
      objectClass,
      functionClass,
      new NimClass(rootShape, "List", objectClass),
      new NimClass(rootShape, "String", objectClass),
      createErrorsModel()
    );
  }

  private NErrorsModel createErrorsModel() {
    return new NErrorsModel(
      new NimClass(rootShape, "Error", objectClass),
      new NimClass(rootShape, "TypeError", objectClass),
      new NimClass(rootShape, "ArgumentError", objectClass),
      new NimClass(rootShape, "ValueError", objectClass)
    );
  }

  private DynamicObject createGlobalScope(DynamicObjectLibrary objectLibrary) {


    NGlobalScopeObject globalScope = new NGlobalScopeObject(rootShape);

    // built-in functions
    NBuiltinDeclarationAccessor.get(NBuiltinFunctions.class).forEach((factory) -> {
      defineBuiltinFunction(objectLibrary, globalScope, factory.key(), factory.value(), factory.regulator());
    });

    // Object class
    NBuiltinDeclarationAccessor.get(NObjectMethods.class).forEach((factory) -> {
      defineBuiltinMethod(objectLibrary, objectClass, factory.key(), factory.value());
    });

    // List class
    NBuiltinDeclarationAccessor.get(NListMethods.class).forEach((factory) -> {
      defineBuiltinMethod(objectLibrary, builtinObjects.listObject, factory.key(), factory.value());
    });

    // String class
    NBuiltinDeclarationAccessor.get(NStringMethods.class).forEach((factory) -> {
      defineBuiltinMethod(objectLibrary, builtinObjects.stringObject, factory.key(), factory.value());
    });

    // global classes
    objectLibrary.putConstant(globalScope, "Object", objectClass, 0);
    objectLibrary.putConstant(globalScope, "String", builtinObjects.stringObject, 0);
    objectLibrary.putConstant(globalScope, "List", builtinObjects.listObject, 0);

    // add all built-in class prototypes to the global scope
    for (Map.Entry<String, NimClass> entry : builtinObjects.builtinClasses.entrySet()) {
      objectLibrary.putConstant(globalScope, entry.getKey(), entry.getValue(), 0);
    }

    // add a constructor to all Error types
    for (Map.Entry<String, NimClass> entry : builtinObjects.errorsModel.ALL.entrySet()) {
      objectLibrary.putConstant(entry.getValue(), "@new",
        // error subtype constructor
        new NFunctionObject(
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
    DynamicObjectLibrary objectLibrary, NGlobalScopeObject globalScope, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory, boolean variadic
  ) {
    objectLibrary.putConstant(
      globalScope,
      name,
      new NFunctionObject(rootShape, functionClass, name, createCallTarget(factory, true), factory.getExecutionSignature().size(), variadic),
      0
    );
  }

  private void defineBuiltinFunction(
    DynamicObjectLibrary objectLibrary, NGlobalScopeObject globalScope, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory
  ) {
    defineBuiltinFunction(objectLibrary, globalScope, name, factory, false);
  }

  private void defineBuiltinMethod(
    DynamicObjectLibrary objectLibrary, NimClass classObject, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory
  ) {
    objectLibrary.putConstant(
      classObject,
      name,
      new NFunctionObject(rootShape, functionClass, name, createCallTarget(factory, false), factory.getExecutionSignature().size() - 1),
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

    var visitor = new NimTranslator(parser, builtinObjects);
    var parseResult = visitor.translate(statements);
    return new NBlockRootNode(
      this, parseResult.frameDescriptor, parseResult.node,
      "@.script", visitor.getRootSourceSection()
    ).getCallTarget();
  }

  @Override
  protected Object getScope(NimContext context) {
    return context.globalScope;
  }
}
