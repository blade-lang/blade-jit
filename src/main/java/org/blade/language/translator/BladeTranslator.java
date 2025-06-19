package org.blade.language.translator;

import com.oracle.truffle.api.bytecode.BytecodeLabel;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.*;
import org.blade.language.parser.BaseVisitor;
import org.blade.language.parser.Parser;
import org.blade.language.parser.ast.AST;
import org.blade.language.parser.ast.Expr;
import org.blade.language.parser.ast.Stmt;
import org.blade.language.runtime.*;
import org.blade.language.shared.BuiltinClassesModel;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BladeTranslator extends BaseVisitor<Void> {
  // Tags
  private static final Class<?>[] EXPRESSION = new Class<?>[]{StandardTags.ExpressionTag.class};
  private static final Class<?>[] READ_VARIABLE = new Class<?>[]{StandardTags.ExpressionTag.class, StandardTags.ReadVariableTag.class};
  private static final Class<?>[] WRITE_VARIABLE = new Class<?>[]{StandardTags.ExpressionTag.class, StandardTags.WriteVariableTag.class};
  private static final Class<?>[] STATEMENT = new Class<?>[]{StandardTags.StatementTag.class};
  private static final Class<?>[] CONDITION = new Class<?>[]{StandardTags.StatementTag.class, StandardTags.ExpressionTag.class};
  private static final Class<?>[] CALL = new Class<?>[]{StandardTags.CallTag.class, StandardTags.ExpressionTag.class};

  // Globals
  private final Parser parser;
  private final Shape objectShape;
  private final NGlobalScopeObjectNode globalScopeNode = NGlobalScopeObjectNodeGen.create();
  private final BladeLanguage language;

  // Bytecode
  private final NBytecodeRootNodeGen.Builder b;

  // Trackers
  private final int localsCount = 0;
  private final Stack<ArrayList<Object>> locals = new Stack<>();
  private final BladeClass currentClass = null;
  ArrayDeque<Class<?>[]> tagStack = new ArrayDeque<>();
  private BytecodeLabel breakLabel;
  private BytecodeLabel continueLabel;

  // State
  private FrameDescriptor.Builder frameDescriptor = FrameDescriptor.newBuilder();
  private ParserState state = ParserState.TOP_LEVEL;
  private int scopeDepth = 0;
  private LocalScope localScope = new LocalScope();

  public BladeTranslator(BladeLanguage language, Parser parser, BuiltinClassesModel classesModel, NBytecodeRootNodeGen.Builder builder) {
    this.parser = parser;
    this.objectShape = classesModel.rootShape;
    this.language = language;
    this.b = builder;

//    // Put the Object class into the local scope so that every class, function, and module
//    // is aware that it exists.
//    Map<String, NFrameMember> objectClasses = new HashMap<>();
//    for (Map.Entry<String, BladeClass> classEntry : classesModel.builtinClasses.entrySet()) {
//      objectClasses.put(classEntry.getKey(), new NFrameMember.ClassObject(classEntry.getValue()));
//    }
//    localScope.push(objectClasses);
  }

  public void translate(List<Stmt> stmtList) {
    b.beginSourceSection(0, parser.lexer.source.getLength());
    b.beginRoot();

    b.beginBlock();
    b.beginTag(StandardTags.RootBodyTag.class);
    int maxIter = stmtList.getLast() instanceof Stmt.Expression ? stmtList.size() - 1 : stmtList.size();

    if (maxIter > 0) {
      b.beginBlock();

      for (int i = 0; i < maxIter; i++) {
        visitStmt(stmtList.get(i));
      }

      b.endBlock();
    }

    b.endTag(StandardTags.RootBodyTag.class);
    b.endBlock();

    b.beginReturn();
    if (stmtList.size() - maxIter > 0) {
      visitStmt(stmtList.getLast());
    } else {
      b.emitLoadConstant(BladeNil.SINGLETON);
    }
    b.endReturn();

    NBytecodeRootNode node = b.endRoot();
    node.setParametersCount(0);
    node.setName(parser.lexer.source.getName());

    b.endSourceSection();
  }

  @Override
  public Void visitStmt(Stmt stmt) {
    beginAttribution(STATEMENT, stmt);
    if (stmt != null) {
      stmt.accept(this);
    }
    endAttribution(STATEMENT);

    return null;
  }

  @Override
  public Void visitExpr(Expr expr) {
    if (expr != null) {
      expr.accept(this);
    }

    return null;
  }

  @Override
  public Void visitNilExpr(Expr.Nil expr) {
    beginAttribution(EXPRESSION, expr);
    b.emitLoadConstant(BladeNil.SINGLETON);
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitBooleanExpr(Expr.Boolean expr) {
    beginAttribution(EXPRESSION, expr);
    b.emitLoadConstant(expr.value);
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitNumberExpr(Expr.Number expr) {
    String number = expr.token.literal();
    beginAttribution(EXPRESSION, expr);

    try {
      if (number.startsWith("0x")) {
        b.emitLoadConstant(Long.parseLong(number.substring(2), 16));
      } else if (number.startsWith("0b")) {
        b.emitLoadConstant(Long.parseLong(number.substring(2), 2));
      } else if (number.startsWith("0c")) {
        b.emitLoadConstant(Long.parseLong(number.substring(2), 8));
      }

      b.emitLoadConstant(Long.parseLong(number));
    } catch (NumberFormatException e) {
      try {
        // Try to convert it to a big integer.
        b.emitLoadConstant(new BigIntObject(new BigInteger(number)));
      } catch (NumberFormatException ignored) {
        // it's possible that the long literal is too big to fit in a 32-bit Java `int` -
        // in that case, and it is not a valid big integer as well, fall back to a double literal
        b.emitLoadConstant(Double.parseDouble(number));
      }
    }

    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitBigNumberExpr(Expr.BigNumber expr) {
    beginAttribution(EXPRESSION, expr);
    b.emitLoadConstant(new BigIntObject(new BigInteger(expr.token.literal())));
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    beginAttribution(EXPRESSION, expr);
    b.emitLoadConstant(BString.fromJavaString(expr.token.literal()));
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitIdentifierExpr(Expr.Identifier expr) {
    String id = expr.token.literal();

    int localId = localScope.getIndex(id);
    beginAttribution(READ_VARIABLE, expr);
    if (localId > -1) {
      b.emitLoadLocal(getLocal(localId));
    } else {
      // default to global value
      b.emitNGetGlobal(BString.fromJavaString(id));
    }
    endAttribution(READ_VARIABLE);

    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    beginAttribution(EXPRESSION, expr);

    switch (expr.op.type()) {
      case PLUS -> {
        b.beginNAdd();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNAdd();
      }
      case MINUS -> {
        b.beginNSubtract();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNSubtract();
      }
      case MULTIPLY -> {
        b.beginNMultiply();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNMultiply();
      }
      case DIVIDE -> {
        b.beginNDivide();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNDivide();
      }
      case FLOOR -> {
        b.beginNFloorDivide();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNFloorDivide();
      }
      case PERCENT -> {
        b.beginNModulo();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNModulo();
      }
      case POW -> {
        b.beginNPow();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNPow();
      }
      case AMP -> {
        b.beginNBitAnd();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNBitAnd();
      }
      case BAR -> {
        b.beginNBitOr();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNBitOr();
      }
      case XOR -> {
        b.beginNBitXor();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNBitXor();
      }
      case LSHIFT -> {
        b.beginNBitLeftShift();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNBitLeftShift();
      }
      case RSHIFT -> {
        b.beginNBitRightShift();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNBitRightShift();
      }
      case URSHIFT -> {
        b.beginNBitUnsignedRightShift();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNBitUnsignedRightShift();
      }
      default -> throw new UnsupportedOperationException(expr.op.literal());
    }

    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    beginAttribution(EXPRESSION, expr);

    switch (expr.op.type()) {
      case MINUS -> {
        b.beginNNegate();
        visitUnboxed(expr.right);
        b.endNNegate();
      }
      case BANG -> {
        b.beginNLogicalNot();
        visitUnboxed(expr.right);
        b.endNLogicalNot();
      }
      case TILDE -> {
        b.beginNBitNot();
        visitUnboxed(expr.right);
        b.endNBitNot();
      }
      default -> throw new UnsupportedOperationException(expr.op.literal());
    }

    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    beginAttribution(EXPRESSION, expr);

    switch (expr.op.type()) {
      case EQUAL_EQ -> {
        b.beginNEqual();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNEqual();
      }
      case BANG_EQ -> {
        b.beginNNotEqual();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNNotEqual();
      }
      case LESS -> {
        b.beginNLessThan();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNLessThan();
      }
      case LESS_EQ -> {
        b.beginNLessThanOrEqual();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNLessThanOrEqual();
      }
      case GREATER -> {
        b.beginNGreaterThan();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNGreaterThan();
      }
      case GREATER_EQ -> {
        b.beginNGreaterThanOrEqual();
        visitUnboxed(expr.left);
        visitUnboxed(expr.right);
        b.endNGreaterThanOrEqual();
      }
      case AND -> {
        b.beginNAnd();
        visitExpr(expr.left);
        visitExpr(expr.right);
        b.endNAnd();
      }
      case OR -> {
        b.beginNOr();
        visitExpr(expr.left);
        visitExpr(expr.right);
        b.endNOr();
      }
      default -> throw new UnsupportedOperationException(expr.op.literal());
    }

    endAttribution(EXPRESSION);
    return null;
  }


  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    visitExpr(expr.expression);
    return null;
  }

//  @Override
//  public Void visitConditionExpr(Expr.Condition expr) {
//    sourceSection(
//      new NConditionalNode(
//        visitExpr(expr.expression),
//        visitExpr(expr.truth),
//        visitExpr(expr.falsy)
//      ), expr
//    );
//  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    if (expr.expression instanceof Expr.Identifier identifier) {
      String name = identifier.token.literal();

      int localId = localScope.getIndex(name);
      beginAttribution(WRITE_VARIABLE, expr);
      if (localId > -1) {
        BytecodeLocal local = getLocal(localId);
        b.beginBlock();
        b.beginStoreLocal(local);
        visitExpr(expr.value);
        b.endStoreLocal();
        b.emitLoadLocal(local);
        b.endBlock();
      } else {
        // default to global value
        b.beginNUpdateGlobal(BString.fromJavaString(name));
        visitExpr(expr.value);
        b.endNUpdateGlobal();
      }
      endAttribution(WRITE_VARIABLE);

      return null;
    } else if (expr.expression instanceof Expr.Index index) {
      // FIXME
      /*sourceSection(
        NListIndexWriteNodeGen.create(
          visitExpr(index.callee),
          visitExpr(index.argument),
          value
        ), expr
      );*/
    }

    throw new BladeRuntimeError("Invalid assignment expression");
  }

//  @Override
//  public Void visitNewExpr(Expr.New expr) {
//    List<NNode> arguments = new ArrayList<>();
//    for (Expr arg : expr.arguments) {
//      arguments.add(visitExpr(arg));
//    }
//
//    sourceSection(NNewExprNodeGen.create(visitExpr(expr.expression), arguments), expr);
//  }
//
//  @Override
//  public Void visitCallExpr(Expr.Call expr) {
//    List<NNode> arguments = new ArrayList<>();
//    if (expr.callee instanceof Expr.Identifier) {
//      arguments.add(new NNilLiteralNode());
//    }
//
//    for (Expr arg : expr.args) {
//      arguments.add(visitExpr(arg));
//    }
//
//    if (expr.callee instanceof Expr.Identifier || expr.callee instanceof Expr.Anonymous) {
//      sourceSection(NFunctionCallExprNodeGen.create(visitExpr(expr.callee), arguments), expr);
//    }
//    sourceSection(new NMethodCallExprNode(visitExpr(expr.callee), arguments), expr);
//  }
//
//  @Override
//  public Void visitSetExpr(Expr.Set expr) {
//    sourceSection(
//      NSetPropertyNodeGen.create(
//        visitExpr(expr.expression),
//        visitExpr(expr.value),
//        expr.name.token.literal()
//      ), expr
//    );
//  }
//
//  @Override
//  public Void visitGetExpr(Expr.Get expr) {
//    sourceSection(NGetPropertyNodeGen.create(visitExpr(expr.expression), expr.name.token.literal()), expr);
//  }
//
//  @Override
//  public Void visitIndexExpr(Expr.Index expr) {
//    sourceSection(NListIndexReadNodeGen.create(visitExpr(expr.callee), visitExpr(expr.argument)), expr);
//  }
//
//  @Override
//  public Void visitSliceExpr(Expr.Slice expr) {
//    NNode callee = visitExpr(expr.callee);
//    NNode lower = expr.lower == null ? new NLongLiteralNode(0) : visitExpr(expr.lower);
//    NNode upper = expr.upper == null ? new NDoubleLiteralNode(0.0) : visitExpr(expr.upper);
//    sourceSection(NGetSliceNodeGen.create(callee, lower, upper), expr);
//  }
//
//  @Override
//  public Void visitArrayExpr(Expr.Array expr) {
//    List<NNode> nodes = new ArrayList<>();
//    for (Expr e : expr.items) {
//      nodes.add(visitExpr(e));
//    }
//    sourceSection(new NListLiteralNode(nodes), expr);
//  }
//
//  @Override
//  public Void visitDictExpr(Expr.Dict expr) {
//    List<NNode> keys = new ArrayList<>();
//    List<NNode> values = new ArrayList<>();
//
//    for (Expr e : expr.keys) {
//      keys.add(visitExpr(e));
//    }
//
//    for (Expr e : expr.values) {
//      values.add(visitExpr(e));
//    }
//
//    sourceSection(new NDictionaryLiteralNode(keys, values), expr);
//  }
//
//  @Override
//  public Void visitRangeExpr(Expr.Range expr) {
//    NRangeLiteralNodeGen.create(visitExpr(expr.lower), visitExpr(expr.upper));
//  }
//
//  @Override
//  public Void visitEchoStmt(Stmt.Echo stmt) {
//    sourceSection(new NEchoStmtNode(visitExpr(stmt.value)), stmt);
//  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    assert stmt.expression != null;
    visitExpr(stmt.expression);
    return null;
  }

  @Override
  public Void visitVarListStmt(Stmt.VarList varList) {
    for (Stmt stmt : varList.declarations) {
      visitStmt(stmt);
    }
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    boolean isConstant = stmt.isConstant;
    String name = stmt.name.literal();

    if (isConstant && stmt.value == null) {
      throw new BladeRuntimeError("Constant '" + name + "' not initialized");
    }

    if (state != ParserState.TOP_LEVEL) {
      if (!localScope.isDeclared(name)) {
        beginAttribution(WRITE_VARIABLE, stmt);
        BytecodeLocal local = getLocal(localScope.add(name));
        b.beginBlock();
        b.beginStoreLocal(local);

        if (stmt.value != null) {
          visitExpr(stmt.value);
        } else {
          b.emitLoadConstant(BladeNil.SINGLETON);
        }

        b.endStoreLocal();
        b.emitLoadLocal(local);
        b.endBlock();
        endAttribution(WRITE_VARIABLE);
      } else {
        throw new BladeRuntimeError("'" + name + "' is already declared in this scope");
      }
    } else {
      // default to global value
      b.beginNSetGlobal(BString.fromJavaString(name), isConstant);

      if (stmt.value != null) {
        visitExpr(stmt.value);
      } else {
        b.emitLoadConstant(BladeNil.SINGLETON);
      }

      b.endNSetGlobal();
    }

    return null;
  }

//  @Override
//  public Void visitPropertyStmt(Stmt.Property stmt) {
//    sourceSection(
//      NSetPropertyNodeGen.create(
//        new NDynamicObjectRefNode(currentClass),
//        stmt.value == null ? new NNilLiteralNode() : sourceSection(visitExpr(stmt.value), stmt.value),
//        stmt.name.literal()
//      ), stmt
//    );
//  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    b.beginBlock();
    newLocalScope(() -> {
      for (Stmt statement : stmt.body) {
        if (statement != null) {
          visitStmt(statement);
        }
      }
    });
    b.endBlock();
    return null;
  }

//  @Override
//  public Void visitIfStmt(Stmt.If stmt) {
//    sourceSection(
//      new NIfStmtNode(
//        visitExpr(stmt.condition),
//        visitStmt(stmt.thenBranch),
//        visitStmt(stmt.elseBranch)
//      ), stmt
//    );
//  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    if (breakLabel == null) {
      throw new BladeRuntimeError("`break` statement used outside of loop");
    }

    b.emitBranch(breakLabel);
    return null;
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    if (continueLabel == null) {
      throw new BladeRuntimeError("`continue` statement used outside of loop");
    }
    b.emitBranch(continueLabel);

    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    BytecodeLabel oldBreak = breakLabel;
    BytecodeLabel oldContinue = continueLabel;

    b.beginBlock();

    breakLabel = b.createLabel();
    continueLabel = b.createLabel();

    b.emitLabel(continueLabel);
    b.beginWhile();

    b.beginNToBooleanNode();
    beginAttribution(CONDITION, stmt.condition);
    visitExpr(stmt.condition);
    endAttribution(CONDITION);
    b.endNToBooleanNode();

    visitStmt(stmt.body);
    b.endWhile();
    b.emitLabel(breakLabel);

    b.endBlock();

    breakLabel = oldBreak;
    continueLabel = oldContinue;

    return null;
  }

  @Override
  public Void visitDoWhileStmt(Stmt.DoWhile stmt) {
    BytecodeLabel oldBreak = breakLabel;
    BytecodeLabel oldContinue = continueLabel;

    b.beginBlock();

    breakLabel = b.createLabel();
    continueLabel = b.createLabel();

    b.emitLabel(continueLabel);
    b.beginWhile();

    visitStmt(stmt.body);

    b.beginNToBooleanNode();
    beginAttribution(CONDITION, stmt.condition);
    visitExpr(stmt.condition);
    endAttribution(CONDITION);
    b.endNToBooleanNode();

    b.endWhile();
    b.emitLabel(breakLabel);

    b.endBlock();

    breakLabel = oldBreak;
    continueLabel = oldContinue;

    return null;
  }

  //  @Override
//  public Void visitIterStmt(Stmt.Iter stmt) {
//    newLocalScope(() -> sourceSection(
//      new NIterStmtNode(
//        stmt.declaration != null ? visitStmt(stmt.declaration) : null,
//        stmt.condition != null ? visitExpr(stmt.condition) : null,
//        stmt.interation != null ? visitExpressionStmt(stmt.interation) : null,
//        visitStmt(stmt.body)
//      ), stmt
//    ));
//  }
//
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    translateFunction(
      stmt,
      stmt.name.literal(),
      stmt.parameters,
      stmt.body,
      globalScopeNode,
      stmt.isVariadic
    );

    return null;
  }


  @Override
  public Void visitMethodStmt(Stmt.Method stmt) {
    translateFunction(
      stmt,
      stmt.name.literal(),
      stmt.parameters,
      stmt.body,
      globalScopeNode,
      stmt.isVariadic
    );
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (state != ParserState.FUNC_DEF) {
      throw new BladeRuntimeError("`return` keyword is not allowed in this scope");
    }

    b.beginReturn();

    if (stmt.value == null) {
      b.emitLoadConstant(BladeNil.SINGLETON);
    } else {
      visitExpr(stmt.value);
    }

    b.endReturn();

    return null;
  }

  //  @Override
//  public Void visitClassStmt(Stmt.Class stmt) {
//    if (state == ParserState.FUNC_DEF) {
//      throw BladeRuntimeError.create("Classes cannot be nested in functions");
//    }
//
//    String className = stmt.name.literal();
//    String superClass = stmt.superclass != null ?
//      stmt.superclass.token.literal() :
//      "Object";
//
//    BladeClass classObject;
//    NFrameMember frameMember = localScope.getFirst().get(superClass);
//    if (frameMember instanceof NFrameMember.ClassObject classMember) {
//      BladeClass superClassObject = classMember.object;
//      classObject = new BladeClass(objectShape, className, superClassObject);
//    } else {
//      throw BladeRuntimeError.create("Class '", className, "' extends unknown or frozen class '", superClass, "'");
//    }
//
//    localScope.getFirst().put(className, new NFrameMember.ClassObject(classObject));
//
//    List<NNode> methods = new ArrayList<>();
//    List<NNode> properties = new ArrayList<>();
//    List<NNode> operators = new ArrayList<>();
//
//    // set the current class
//    currentClass = classObject;
//
//    for (Stmt.Property property : stmt.properties) {
//      properties.add(visitPropertyStmt(property));
//    }
//
//    for (Stmt.Method method : stmt.methods) {
//      methods.add(translateFunction(
//        method,
//        method.name.literal(),
//        method.parameters,
//        method.body,
//        new NDynamicObjectRefNode(classObject),
//        method.isVariadic
//      ));
//    }
//
//    for (Stmt.Method method : stmt.operators) {
//      methods.add(translateFunction(
//        method,
//        method.name.literal(),
//        method.parameters,
//        method.body,
//        new NDynamicObjectRefNode(classObject),
//        method.isVariadic
//      ));
//    }
//
//    // reset current class
//    currentClass = null;
//
//    // deliberately not wrapped in sourceSection so that debuggers won't stop
//    // in class declarations and their global variable
//    sourceSection(
//      NGlobalDeclNodeGen.create(
//        NGlobalScopeObjectNodeGen.create(),
//        sourceSection(new NClassDeclNode(methods, properties, operators, classObject), stmt),
//        className,
//        false
//      ), stmt
//    );
//  }
//
//  @Override
//  public Void visitSelfExpr(Expr.Self expr) {
//    if (currentClass == null) {
//      throw BladeRuntimeError.create("`self` keyword not allowed outside a class");
//    }
//
//    sourceSection(new NSelfLiteralNode(), expr);
//  }
//
//  @Override
//  public Void visitParentExpr(Expr.Parent expr) {
//    if (currentClass == null) {
//      throw BladeRuntimeError.create("`parent` keyword not allowed outside a class");
//    }
//
//    sourceSection(new NParentExprNode(currentClass), expr);
//  }
//
//  @Override
//  public Void visitRaiseStmt(Stmt.Raise stmt) {
//    sourceSection(NRaiseStmtNodeGen.create(visitExpr(stmt.exception), false), stmt);
//  }
//
//  @Override
//  public Void visitAssertStmt(Stmt.Assert stmt) {
//    sourceSection(
//      new NAssertStmtNode(
//        sourceSection(visitExpr(stmt.expression), stmt.expression),
//        sourceSection(
//          NRaiseStmtNodeGen.create(
//            stmt.message == null || stmt.message instanceof Expr.Literal ?
//              sourceSection(
//                NNewExprNodeGen.create(
//                  NGetGlobalNodeGen.create(globalScopeNode, "AssertError"),
//                  List.of(new NStringLiteralNode(stmt.message instanceof Expr.Literal literal ?
//                    literal.token.literal() :
//                    "Failed assertion"
//                  ))
//                ), stmt
//              ) :
//              visitExpr(stmt.message),
//            true
//          ), stmt.message == null ? stmt : stmt.message
//        )
//      ), stmt
//    );
//  }
//
//  @Override
//  public Void visitCatchStmt(Stmt.Catch stmt) {
//    NNode body = visitBlockStmt(stmt.body);
//    NNode thenBody = stmt.finallyBody == null ? null : visitBlockStmt(stmt.finallyBody);
//    NNode asBody = null;
//    int slot = -1;
//
//    if (stmt.name != null) {
//
//      String errorName = stmt.name.token.literal();
//      LocalRefSlot slotId = new LocalRefSlot(errorName, ++localsCount);
//      slot = frameDescriptor.addSlot(FrameSlotKind.Object, slotId, 1);
//      if (localScope.peek().putIfAbsent(errorName, new NFrameMember.LocalVariable(slot, true)) != null) {
//        throw BladeRuntimeError.error(thenBody, "'", errorName, "' is already declared in this scope");
//      }
//
//      // parse the 'catch' statement block
//      asBody = visitBlockStmt(stmt.catchBody);
//    }
//
//    new NTryCatchStmtNode(body, slot, asBody, thenBody);
//  }
//
//  @Override
//  public Void visitAnonymousExpr(Expr.Anonymous expr) {
//    new NAnonymousExprNode(
//      translateFunction(
//        expr.function,
//        "@anonymous",
//        expr.function.parameters,
//        expr.function.body,
//        globalScopeNode,
//        expr.function.isVariadic
//      )
//    );
//  }
//
//  @Override
//  public Void visitUsingStmt(Stmt.Using stmt) {
//    NNode value = visitExpr(stmt.expression);
//
//    int casesLength = stmt.caseLabels.size();
//    NWhenNode[] cases = new NWhenNode[casesLength];
//
//    for (int i = 0; i < casesLength; i++) {
//      cases[i] = new NWhenNode(visitExpr(stmt.caseLabels.get(i)), visitStmt(stmt.caseBodies.get(i)));
//    }
//
//    NNode defaultCase = null;
//    if (stmt.defaultCase != null) {
//      defaultCase = visitStmt(stmt.defaultCase);
//    }
//
//    new NUsingNode(value, cases, defaultCase);
//  }
//
//  @Override
//  public Void visitImportStmt(Stmt.Import stmt) {
//    NNode name = visitExpr(stmt.name);
//    String moduleName = stmt.name.token.literal();
//
//    String currentSourcePath;
//    if(parser.lexer.source.getPath() != null) {
//      currentSourcePath = parser.lexer.source.getPath();
//    } else {
//      // We're in the REPL.
//      currentSourcePath = new File(".").getAbsolutePath();
//    }
//
//    File currentDir = new File(currentSourcePath).getParentFile();
//    String sep = File.separator;
//
//    File moduleFile;
//
//    // TODO: Handle importing built-in modules.
//
//    if(stmt.path.startsWith(".")) {
//      moduleFile = new File(currentDir, stmt.path + ".b");
//      if (!moduleFile.exists()) {
//        moduleFile = new File(currentDir, String.join(sep, stmt.path, "index.b"));
//        if (!moduleFile.exists()) {
//          // That's all for relative import
//          throw BladeRuntimeError.error(
//            name,
//            "Module '" + moduleName + "' not found"
//          );
//        }
//      }
//    } else {
//      File baseRootDirectory = null;
//      if (TruffleOptions.AOT) {
//        baseRootDirectory = new File(ProcessProperties.getExecutableName()).getParentFile();
//      } else {
//        try {
//          baseRootDirectory = Path.of(
//            Main.class.getProtectionDomain()
//              .getCodeSource()
//              .getLocation()
//              .toURI()
//          ).getParent().toFile();
//        } catch (URISyntaxException ignored) {}
//      }
//
//      // Non-relative imports start from the `.blade/libs` directory in the current directory
//      // and progress to the application root directory.
//      moduleFile = new File(currentDir, String.join(sep, ".blade", "libs", stmt.path + ".b"));
//      if (!moduleFile.exists()) {
//        moduleFile = new File(currentDir, String.join(sep, ".blade", "libs", stmt.path, "index.b"));
//        if (!moduleFile.exists()) {
//
//          if (baseRootDirectory == null || !baseRootDirectory.exists()) {
//            throw BladeRuntimeError.error(
//              name,
//              "Module '" + moduleName + "' not found"
//            );
//          }
//
//          // Next progress to the `apps` directory of the Blade installation.
//          // This is where the user's global libraries live, and they can be used
//          // to override the built-in libraries. So we're starting here...
//          File rootDirectory = new File(baseRootDirectory, "apps");
//
//          moduleFile = new File(rootDirectory, stmt.path + ".b");
//          if (!moduleFile.exists()) {
//            moduleFile = new File(rootDirectory, String.join(sep, stmt.path, "index.b"));
//          }
//
//
//          if (!moduleFile.exists()) {
//            // If we still haven't found the module,
//            // We can start checking the Blade's standard library directory.
//            rootDirectory = new File(baseRootDirectory, "libs");
//
//            moduleFile = new File(rootDirectory, stmt.path + ".b");
//            if (!moduleFile.exists()) {
//              moduleFile = new File(rootDirectory, String.join(sep, stmt.path, "index.b"));
//              if (!moduleFile.exists()) {
//
//                // We've checked everywhere and still can't find it.
//                throw BladeRuntimeError.error(
//                  name,
//                  "Module '" + moduleName + "' not found"
//                );
//              }
//            }
//          }
//        }
//      }
//    }
//
//    String[] elements = new String[stmt.elements.size()];
//    for (int i = 0; i < stmt.elements.size(); i++) {
//      elements[i] = stmt.elements.get(i).token.literal();
//    }
//
//    // module file exists
//    new NImportNode(
//      new NStringLiteralNode(moduleFile.getAbsolutePath()),
//      new NStringLiteralNode(moduleName),
//      elements,
//      stmt.all
//    );
//  }
//
  private NNode translateFunction(Stmt source, String name, List<Expr.Identifier> parameters, Stmt.Block body, NNode root, boolean isVariadic) {
//    FrameDescriptor.Builder previousFrameDescriptor = frameDescriptor;
//    ParserState previousState = state;
//    var previousLocalScopes = localScope;
//
//    this.frameDescriptor = FrameDescriptor.newBuilder();
//    this.state = ParserState.FUNC_DEF;
//    this.localScope = new LocalScope(previousLocalScopes, ++scopeDepth);
//
//    frameDescriptor.addSlot(FrameSlotKind.Object, new LocalRefSlot(name, ++localsCount), 0);
//
//    // for the `self` objects
//    frameDescriptor.addSlot(FrameSlotKind.Object, new LocalRefSlot("self", ++localsCount), 0);
//
//    Map<String, NFrameMember> localVariables = new HashMap<>();
//    for (int i = 0; i < parameters.size(); i++) {
//      String param = parameters.get(i).token.literal();
//      localVariables.put(param, new NFrameMember.FunctionArgument(i + 1));
//
//      frameDescriptor.addSlot(FrameSlotKind.Illegal, new LocalRefSlot(param, ++localsCount), 0);
//    }
//    this.localScope.push(localVariables);

//    NBlockStmtNode statements = visitBlockStmt(body);
//
//    boolean captured = localScope.captures;
//
//    FrameDescriptor frameDescriptor = this.frameDescriptor.build();
//    this.frameDescriptor = previousFrameDescriptor;
//    this.state = previousState;
//    this.localScope = previousLocalScopes;
//    scopeDepth--;
//
//    sourceSection(
//      NFunctionStmtNodeGen.create(
//        root,
//        name,
//        frameDescriptor,
//        (NFunctionBodyNode) sourceSection(new NFunctionBodyNode(statements), body),
//        parameters.size(),
//        isVariadic ? 1 : 0,
//        captured ? 1 : 0
//      ), source
//    );

    ParserState previousState = state;
    var previousLocalScopes = localScope;

    locals.push(new ArrayList<>());

    this.frameDescriptor = FrameDescriptor.newBuilder();
    this.localScope = new LocalScope(previousLocalScopes, ++scopeDepth);
    this.state = ParserState.FUNC_DEF;

    SourceSection sourceSection = getSourceSection(source);
    b.beginSourceSection(sourceSection.getCharIndex(), sourceSection.getCharLength());
    b.beginRoot();

    b.beginBlock();
    int paramsCount = parameters.size();
    for (int i = 0; i < paramsCount; i++) {
      Expr.Identifier paramAst = parameters.get(i);
      String paramName = paramAst.token.literal();
      localScope.declare(paramName);

      BytecodeLocal argLocal = b.createLocal(paramName, null);
      locals.peek().add(argLocal);

      b.beginStoreLocal(argLocal);
      beginSourceSection(paramAst);
      b.emitNLoadArgument(i);
      b.endSourceSection();
      b.endStoreLocal();
    }

    b.beginTag(StandardTags.RootBodyTag.class);
    b.beginBlock();
    visitBlockStmt(body);
    locals.pop();
    scopeDepth--;

    this.state = previousState;
    this.localScope = previousLocalScopes;

    b.endBlock();

    b.endTag(StandardTags.RootBodyTag.class);
    b.endBlock();

    b.beginReturn();
    b.emitLoadConstant(BladeNil.SINGLETON);
    b.endReturn();

    NBytecodeRootNode node = b.endRoot();
    node.setParametersCount(paramsCount);
    node.setName(name);

    b.endSourceSection();

    return null;
  }

  private void newLocalScope(Callback callback) {
    ParserState previousState = state;
    var previousLocalScopes = localScope;

    this.frameDescriptor = FrameDescriptor.newBuilder();
    this.localScope = new LocalScope(previousLocalScopes, ++scopeDepth);

    if (state == ParserState.TOP_LEVEL) {
      state = ParserState.NESTED_TOP_LEVEL;
    }

    callback.run();

    state = previousState;
    localScope = previousLocalScopes;
    scopeDepth--;
  }

//  private NNode sourceSection(NNode node, Object object) {
//    if (object instanceof AST ast) {

  ////      System.out.println("SL = " +ast.startLine+", EL = " +ast.endLine+", SC = " + ast.startColumn + ", EC = " +ast.endColumn);
//      node.setSourceSection(parser.lexer.source.createSection(
//        ast.startLine, ast.startColumn + 1,
//        ast.endLine, ast.endColumn + 1
//      ));
//    }
//
//    node.setSourceSection(parser.lexer.source.createSection(1));
//  }
//
//  public SourceSection getRootSourceSection() {
//    parser.lexer.source.createSection(0, parser.lexer.source.getLength());
//  }
  private BytecodeLocal getLocal(int index) {
    Object local = locals.peek().get(index);
    if (local instanceof String s) {
      local = b.createLocal(s, null);
      locals.peek().set(index, local);
    }
    return (BytecodeLocal) local;
  }

  private void beginSourceSection(int start, int end) {
    int length = end - start + 1;
    assert length >= 0;
    b.beginSourceSection(start, length);
  }

  private void beginSourceSection(AST ast) throws AssertionError {
    SourceSection sourceSection = getSourceSection(ast);
    beginSourceSection(sourceSection.getCharIndex(), sourceSection.getCharLength());
  }

  private void beginAttribution(Class<?>[] tags, AST ast) {
    SourceSection sourceSection = getSourceSection(ast);
    beginAttribution(tags, sourceSection.getCharIndex(), sourceSection.getCharLength());
  }

  private void beginAttribution(Class<?>[] tags, int start, int end) {
    boolean parentCondition = tagStack.peek() == CONDITION;
    tagStack.push(tags);
    if (parentCondition) {
      return;
    }
    beginSourceSection(start, end);
    b.beginTag(tags);
  }

  private void endAttribution(Class<?>[] tags) {
    tagStack.pop();
    boolean parentCondition = tagStack.peek() == CONDITION;
    if (parentCondition) {
      return;
    }
    b.endTag(tags);
    b.endSourceSection();
  }

  private void visitUnboxed(Expr expr) {
    if (needsUnboxing(expr)) {
      // skip unboxing for constants
      b.beginNUnbox();
      visitExpr(expr);
      b.endNUnbox();
    } else {
      visitExpr(expr);
    }
  }

  private boolean needsUnboxing(AST ast) {
    if (ast instanceof Expr.Number || ast instanceof Expr.Literal || ast instanceof Expr.BigNumber) {
      // constants are guaranteed to be already unboxed
      return false;
    }

    if (ast instanceof Expr.Unary unary) {
      return needsUnboxing(unary.right);
    }

    if (ast instanceof Expr.Binary binary) {
      return needsUnboxing(binary.left) || needsUnboxing(binary.right);
    }

    if (ast instanceof Expr.Logical logical) {
      return needsUnboxing(logical.left) || needsUnboxing(logical.right);
    }

    if (ast instanceof Expr.Index index) {
      return needsUnboxing(index.callee) || needsUnboxing(index.argument);
    }

    if (ast instanceof Expr.Call call) {
      return needsUnboxing(call.callee) || call.args.stream().anyMatch(this::needsUnboxing);
    }

    if (ast instanceof Expr.Get get) {
      return needsUnboxing(get.expression);
    }

    if (ast instanceof Expr.Set set) {
      return needsUnboxing(set.expression) || needsUnboxing(set.value);
    }

    return false;
  }

  private SourceSection getSourceSection(AST ast) {
    return parser.lexer.source.createSection(
      ast.startLine, ast.startColumn + 1,
      ast.endLine, ast.endColumn + 1
    );
  }

  // State management
  private enum ParserState {TOP_LEVEL, NESTED_TOP_LEVEL, FUNC_DEF}

  interface Callback {
    void run();
  }

}
