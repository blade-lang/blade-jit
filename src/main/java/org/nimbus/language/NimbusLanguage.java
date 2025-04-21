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
import org.nimbus.language.builtins.AbsBuiltinFunctionNodeFactory;
import org.nimbus.language.builtins.MicroTimeBuiltinFunctionNodeFactory;
import org.nimbus.language.builtins.TimeBuiltinFunctionNode;
import org.nimbus.language.builtins.TimeBuiltinFunctionNodeFactory;
import org.nimbus.language.builtins.object.NObjectHasPropMethodNodeFactory;
import org.nimbus.language.builtins.object.NObjectToStringMethodNodeFactory;
import org.nimbus.language.builtins.string.NStringIndexOfMethodNodeFactory;
import org.nimbus.language.builtins.string.NStringUpperMethodNodeFactory;
import org.nimbus.language.nodes.NBlockRootNode;
import org.nimbus.language.nodes.NRootNode;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.nodes.functions.NFunctionRootNode;
import org.nimbus.language.nodes.functions.NReadFunctionArgsExprNode;
import org.nimbus.language.nodes.statements.NBlockStmtNode;
import org.nimbus.language.parser.Lexer;
import org.nimbus.language.parser.Parser;
import org.nimbus.language.parser.ast.Stmt;
import org.nimbus.language.runtime.*;
import org.nimbus.language.shared.NBuiltinClassesModel;
import org.nimbus.language.translator.NimTranslator;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
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
  private final NimClass listClass = new NimClass(rootShape, "List", objectClass);

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
      createGlobalScope(objectLibrary),
      createBuiltinClasses(objectLibrary),
      new NFunctionObject(
        rootShape,
        functionClass,
        "<>",
        new NBlockRootNode(
          this,
          FrameDescriptor.newBuilder().build(),
          new NBlockStmtNode(Collections.emptyList())
        ).getCallTarget(),
        0)
    );
  }

  private NBuiltinClassesModel createBuiltinClasses(DynamicObjectLibrary objectLibrary) {
    return new NBuiltinClassesModel(
      rootShape,
      listShape,
      objectClass,
      functionClass,
      listClass,
      createStringClass(objectLibrary)
    );
  }

  private NimClass createStringClass(DynamicObjectLibrary objectLibrary) {
    NimClass stringClass = new NimClass(rootShape, "String", objectClass);

    defineBuiltinMethod(objectLibrary, stringClass, "upper", NStringUpperMethodNodeFactory.getInstance());
    defineBuiltinMethod(objectLibrary, stringClass, "index_of", NStringIndexOfMethodNodeFactory.getInstance());

    return stringClass;
  }

  private DynamicObject createGlobalScope(DynamicObjectLibrary objectLibrary) {
    NGlobalScopeObject globalScope = new NGlobalScopeObject(rootShape);

    // built-in functions
    defineBuiltinFunction(objectLibrary, globalScope, "abs", AbsBuiltinFunctionNodeFactory.getInstance());
    defineBuiltinFunction(objectLibrary, globalScope, "time", TimeBuiltinFunctionNodeFactory.getInstance());
    defineBuiltinFunction(objectLibrary, globalScope, "microtime", MicroTimeBuiltinFunctionNodeFactory.getInstance());

    // Object class
    defineBuiltinMethod(objectLibrary, objectClass, "has_prop", NObjectHasPropMethodNodeFactory.getInstance());
    defineBuiltinMethod(objectLibrary, objectClass, "to_string", NObjectToStringMethodNodeFactory.getInstance());

    // global classes
    objectLibrary.putConstant(globalScope, "Object", objectClass, 0);

    return globalScope;
  }

  private void defineBuiltinFunction(
    DynamicObjectLibrary objectLibrary, NGlobalScopeObject globalScope, String name,
    NodeFactory<? extends NBuiltinFunctionNode> factory
  ) {
    objectLibrary.putConstant(
      globalScope,
      name,
      new NFunctionObject(rootShape, functionClass, name, createCallTarget(factory, true), factory.getExecutionSignature().size()),
      0
    );
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
      .mapToObj(i -> new NReadFunctionArgsExprNode(offset ? i + 1 : i))
      .toArray(NReadFunctionArgsExprNode[]::new);

    NFunctionRootNode rootNode = new NFunctionRootNode(this, factory.createNode((Object) arguments));

    return rootNode.getCallTarget();
  }

  @Override
  protected CallTarget parse(ParsingRequest request) throws Exception {
    Source source = request.getSource();

    Parser parser = new Parser(new Lexer(source));
    List<Stmt> statements = parser.parse();

    var visitor = new NimTranslator(rootShape, objectClass);
    var parseResult = visitor.translate(statements);
    return new NRootNode(this, parseResult.frameDescriptor, parseResult.node).getCallTarget();
  }

  @Override
  protected Object getScope(NimContext context) {
    return context.globalScope;
  }
}
