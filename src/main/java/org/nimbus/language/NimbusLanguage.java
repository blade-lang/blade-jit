package org.nimbus.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.builtins.AbsBuiltinFunctionNodeFactory;
import org.nimbus.language.builtins.string.NStringIndexOfMethodNodeFactory;
import org.nimbus.language.builtins.string.NStringUpperMethodNodeFactory;
import org.nimbus.language.nodes.NBuiltinFunctionNode;
import org.nimbus.language.nodes.NRootNode;
import org.nimbus.language.nodes.calls.NReadFunctionArgsExprNode;
import org.nimbus.language.nodes.calls.NFunctionRootNode;
import org.nimbus.language.parser.Lexer;
import org.nimbus.language.parser.Parser;
import org.nimbus.language.parser.ast.Stmt;
import org.nimbus.language.runtime.*;
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

  // Shapes
  public final Shape rootShape = Shape.newBuilder().build();
  public final Shape listShape = createShape(NListObject.class);

  private final DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();

  private void declareBuiltinFunction(NimContext context, String name, NodeFactory<? extends NBuiltinFunctionNode> factory) {
    objectLibrary.putConstant(
      context.globalScope,
      name,
      defineBuiltinFunction(name, factory),
      0
    );
  }

  private CallTarget createCallTarget(NodeFactory<? extends NBuiltinFunctionNode> factory) {
    int argumentCount = factory.getExecutionSignature().size();

    NReadFunctionArgsExprNode[] arguments = IntStream.range(0, argumentCount)
      .mapToObj(NReadFunctionArgsExprNode::new)
      .toArray(NReadFunctionArgsExprNode[]::new);

    NFunctionRootNode rootNode = new NFunctionRootNode(this,
      factory.createNode((Object) arguments));

    return rootNode.getCallTarget();
  }

  private NFunctionObject defineBuiltinFunction(String name, NodeFactory<? extends NBuiltinFunctionNode> factory) {
    return new NFunctionObject(name, createCallTarget(factory), factory.getExecutionSignature().size());
  }

  private void installBuiltinFunctions(NimContext context) {
    declareBuiltinFunction(context, "abs", AbsBuiltinFunctionNodeFactory.getInstance());
  }

  private Shape createShape(Class<? extends NBaseObject> layout) {
    return Shape.newBuilder()
      .allowImplicitCastIntToLong(true)
      .layout(layout, MethodHandles.lookup())
      .build();
  }

  public NStringPrototype createStringPrototype() {
    return new NStringPrototype(
      createCallTarget(NStringUpperMethodNodeFactory.getInstance()),
      createCallTarget(NStringIndexOfMethodNodeFactory.getInstance())
    );
  }

  @Override
  protected NimContext createContext(Env env) {
    NimContext context = new NimContext(this);
    installBuiltinFunctions(context);
    return  context;
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
