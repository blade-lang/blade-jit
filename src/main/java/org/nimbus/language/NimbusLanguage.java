package org.nimbus.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.builtins.AbsBuiltinFunctionNodeFactory;
import org.nimbus.language.builtins.string.NStringIndexOfMethodNodeFactory;
import org.nimbus.language.builtins.string.NStringUpperMethodNodeFactory;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.nodes.NRootNode;
import org.nimbus.language.nodes.functions.NReadFunctionArgsExprNode;
import org.nimbus.language.nodes.functions.NFunctionRootNode;
import org.nimbus.language.parser.Lexer;
import org.nimbus.language.parser.Parser;
import org.nimbus.language.parser.ast.Stmt;
import org.nimbus.language.runtime.*;
import org.nimbus.language.shared.NBuiltinClassesModel;
import org.nimbus.language.translator.NimTranslator;

import java.lang.invoke.MethodHandles;
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

  private static final LanguageReference<NimbusLanguage> REFERENCE = LanguageReference.create(NimbusLanguage.class);
  public final static TruffleString.Encoding ENCODING = TruffleString.Encoding.UTF_8;

  private final DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();

  // Shapes
  public final Shape rootShape = Shape.newBuilder().build();
  public final Shape listShape = createShape(NListObject.class);

  // models
  private final NClassObject functionClass = new NClassObject(rootShape, "Function");
  private final NClassObject listClass = new NClassObject(rootShape, "List");

  private Shape createShape(Class<? extends NClassInstance> layout) {
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
      createBuiltinClasses(objectLibrary)
    );
  }

  private NBuiltinClassesModel createBuiltinClasses(DynamicObjectLibrary objectLibrary) {
    return new NBuiltinClassesModel(
      rootShape,
      listShape,
      functionClass,
      listClass,
      createStringClass(objectLibrary)
    );
  }

  private NClassObject createStringClass(DynamicObjectLibrary objectLibrary) {
    NClassObject stringClass = new NClassObject(rootShape, "String");

    defineBuiltinMethod(objectLibrary, stringClass, "upper", NStringUpperMethodNodeFactory.getInstance());
    defineBuiltinMethod(objectLibrary, stringClass, "index_of", NStringIndexOfMethodNodeFactory.getInstance());

    return stringClass;
  }

  private DynamicObject createGlobalScope(DynamicObjectLibrary objectLibrary) {
    NGlobalScopeObject globalScope = new NGlobalScopeObject(rootShape);

    defineBuiltinFunction(objectLibrary, globalScope, "abs", AbsBuiltinFunctionNodeFactory.getInstance());

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
    DynamicObjectLibrary objectLibrary, NClassObject classObject, String name,
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

    var visitor = new NimTranslator(rootShape, listShape);
    var parseResult = visitor.translate(statements);
    return new NRootNode(this, parseResult.frameDescriptor, parseResult.node).getCallTarget();
  }

  @Override
  protected Object getScope(NimContext context) {
    return context.globalScope;
  }

  public static NimbusLanguage get(Node node) {
    return REFERENCE.get(node);
  }
}
