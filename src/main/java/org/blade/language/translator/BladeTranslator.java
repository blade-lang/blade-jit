package org.blade.language.translator;

import com.oracle.truffle.api.bytecode.BytecodeLabel;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
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
  private final NGlobalScopeObjectNode globalScopeNode = NGlobalScopeObjectNodeGen.create();
  private final BladeLanguage language;

  // Bytecode
  private final NBytecodeRootNodeGen.Builder b;

  // Trackers
  private final Stack<ArrayList<Object>> locals = new Stack<>();
  private final BladeClass currentClass = null;
  ArrayDeque<Class<?>[]> tagStack = new ArrayDeque<>();
  private BytecodeLabel breakLabel;
  private BytecodeLabel continueLabel;
  private int anonymousCount = 0;

  // State
  private ParserState state = ParserState.TOP_LEVEL;
  private int scopeDepth = 0;
  private LocalScope localScope = new LocalScope();

  public BladeTranslator(BladeLanguage language, Parser parser, BuiltinClassesModel classesModel, NBytecodeRootNodeGen.Builder builder) {
    this.parser = parser;
    this.language = language;
    this.b = builder;
    locals.push(new ArrayList<>());
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
    node.setName(BString.fromJavaString("<script>"));

    b.endSourceSection();
  }

  @Override
  public Void visitStmt(Stmt stmt) {
    if (stmt != null) {
      stmt.accept(this);
    }

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
      } else {
        b.emitLoadConstant(Long.parseLong(number));
      }
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
    TruffleString id = BString.fromJavaString(expr.token.literal());

    int localId = localScope.getIndex(id);
    beginAttribution(READ_VARIABLE, expr);
    if (localId > -1) {
      b.emitLoadLocal(getLocal(localId));
    } else {
      // default to global value
      b.emitNGetGlobal(id);
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

  @Override
  public Void visitConditionExpr(Expr.Condition expr) {
    beginAttribution(EXPRESSION, expr);
    b.beginConditional();

    beginAttribution(CONDITION, expr.expression);
    b.beginNToBooleanNode();
    visitExpr(expr.expression);
    b.endNToBooleanNode();
    endAttribution(CONDITION);

    visitExpr(expr.truth);
    visitExpr(expr.falsy);

    b.endConditional();
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    if (expr.expression instanceof Expr.Identifier identifier) {
      TruffleString name = BString.fromJavaString(identifier.token.literal());

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
        b.beginNUpdateGlobal(name);
        visitExpr(expr.value);
        b.endNUpdateGlobal();
      }
      endAttribution(WRITE_VARIABLE);

      return null;
    } else if (expr.expression instanceof Expr.Index index) {
      beginAttribution(WRITE_VARIABLE, index);
      b.beginNSetIndex();
      visitExpr(index.callee);
      visitExpr(index.argument);
      visitExpr(expr.value);
      b.endNSetIndex();
      endAttribution(WRITE_VARIABLE);
    }

    return null;
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

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    beginAttribution(CALL, expr);
    b.beginNDefCall(expr.args.size());
    visitExpr(expr.callee);
    for (Expr arg : expr.args) {
      visitUnboxed(arg);
    }
    b.endNDefCall();
    endAttribution(CALL);
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    beginAttribution(EXPRESSION, expr);
    b.beginNSetProperty(BString.fromJavaString(expr.name.token.literal()));
    visitExpr(expr.expression);
    visitExpr(expr.value);
    b.endNSetProperty();
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    beginAttribution(EXPRESSION, expr);
    b.beginNGetProperty(BString.fromJavaString(expr.name.token.literal()));
    visitExpr(expr.expression);
    b.endNGetProperty();
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitIndexExpr(Expr.Index expr) {
    beginAttribution(EXPRESSION, expr);
    b.beginNGetIndex();
    visitExpr(expr.callee);
    visitExpr(expr.argument);
    b.endNGetIndex();
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitSliceExpr(Expr.Slice expr) {
    beginAttribution(EXPRESSION, expr);
    b.beginNGetSlice();
    visitExpr(expr.callee);
    if(expr.lower == null) {
      b.emitLoadConstant(0L);
    } else {
      visitExpr(expr.lower);
    }
    if(expr.upper == null) {
      // FIXME: Replace with get property length of callee operation.
      b.emitLoadConstant(-1L);
    } else {
      visitExpr(expr.upper);
    }
    b.endNGetSlice();
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitArrayExpr(Expr.Array expr) {
    beginAttribution(EXPRESSION, expr);
    b.beginNCreateList();
    for (Expr e : expr.items) {
      visitExpr(e);
    }
    b.endNCreateList();
    endAttribution(EXPRESSION);
    return null;
  }

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

  @Override
  public Void visitRangeExpr(Expr.Range expr) {
    beginAttribution(EXPRESSION, expr);
    b.beginNCreateRange();
    if(expr.lower == null) {
      b.emitLoadConstant(0);
    } else {
      visitExpr(expr.lower);
    }
    if(expr.upper == null) {
      b.emitLoadConstant(expr.lower == null ? 1 : 0);
    } else {
      visitExpr(expr.upper);
    }
    b.endNCreateRange();
    endAttribution(EXPRESSION);
    return null;
  }

  @Override
  public Void visitEchoStmt(Stmt.Echo stmt) {
    beginAttribution(STATEMENT, stmt);
    b.beginNEcho();
    visitExpr(stmt.value);
    b.endNEcho();
    endAttribution(STATEMENT);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    assert stmt.expression != null;
    beginAttribution(STATEMENT, stmt);
    visitExpr(stmt.expression);
    endAttribution(STATEMENT);
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
    TruffleString name = BString.fromJavaString(stmt.name.literal());

    if (isConstant && stmt.value == null) {
      throw new BladeRuntimeError("Constant '" + name + "' not initialized");
    }

    beginAttribution(WRITE_VARIABLE, stmt.name.offset(), stmt.name.length());
    if (state != ParserState.TOP_LEVEL) {
      if (!localScope.isDeclared(name)) {
        localScope.declare(name, locals.peek().size());

        BytecodeLocal local = b.createLocal(stmt.name.literal(), null);
        locals.peek().add(local);

        b.beginBlock();
        b.beginStoreLocal(local);

        if (stmt.value != null) {
          visitExpr(stmt.value);
        } else {
          b.emitLoadConstant(BladeNil.SINGLETON);
        }

        b.endStoreLocal();
        b.endBlock();
      } else {
        throw new BladeRuntimeError("'" + name + "' is already declared in this scope");
      }
    } else {
      // default to global value
      b.beginBlock();
      b.beginNSetGlobal(name, isConstant);

      if (stmt.value != null) {
        visitExpr(stmt.value);
      } else {
        b.emitLoadConstant(BladeNil.SINGLETON);
      }

      b.endNSetGlobal();
      b.endBlock();
    }
    endAttribution(WRITE_VARIABLE);

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

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (stmt.elseBranch == null) {
      b.beginIfThen();

      beginAttribution(CONDITION, stmt.condition);
      b.beginNToBooleanNode();
      visitExpr(stmt.condition);
      b.endNToBooleanNode();
      endAttribution(CONDITION);

      visitStmt(stmt.thenBranch);
      b.endIfThen();
    } else {
      b.beginIfThenElse();

      beginAttribution(CONDITION, stmt.condition);
      b.beginNToBooleanNode();
      visitExpr(stmt.condition);
      b.endNToBooleanNode();
      endAttribution(CONDITION);

      if(!(stmt.thenBranch instanceof Stmt.Block)) {
        b.beginBlock();
        visitStmt(stmt.thenBranch);
        b.endBlock();
      } else {
        visitStmt(stmt.thenBranch);
      }

      if(!(stmt.elseBranch instanceof Stmt.Block)) {
        b.beginBlock();
        visitStmt(stmt.elseBranch);
        b.endBlock();
      } else {
        visitStmt(stmt.elseBranch);
      }

      b.endIfThenElse();
    }

    return null;
  }

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

    visitStmt(stmt.body);

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
  public Void visitIterStmt(Stmt.Iter stmt) {
      newLocalScope(() -> {
        BytecodeLabel oldBreak = breakLabel;
        BytecodeLabel oldContinue = continueLabel;

        b.beginBlock();

        if(stmt.declaration != null) {
          visitStmt(stmt.declaration);
        }

        breakLabel = b.createLabel();
        b.beginWhile();

        if(stmt.condition != null) {
          b.beginNToBooleanNode();
          beginAttribution(CONDITION, stmt.condition);
          visitExpr(stmt.condition);
          endAttribution(CONDITION);
          b.endNToBooleanNode();
        } else {
          beginAttribution(CONDITION, stmt);
          b.emitLoadConstant(true);
          endAttribution(CONDITION);
        }

        b.beginBlock();
        continueLabel = b.createLabel();

        visitStmt(stmt.body);

        b.emitLabel(continueLabel);
        if(stmt.iteration != null) {
          visitExpressionStmt(stmt.iteration);
        }
        b.endBlock();

        b.endWhile();
        b.emitLabel(breakLabel);

        b.endBlock();

        breakLabel = oldBreak;
        continueLabel = oldContinue;
      });
      return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    translateFunction(
      stmt,
      stmt.name.literal(),
      stmt.parameters,
      stmt.body,
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

  @Override
  public Void visitAnonymousExpr(Expr.Anonymous expr) {
    translateFunction(
      expr.function,
      "@anonymous" + anonymousCount++,
      expr.function.parameters,
      expr.function.body,
      expr.function.isVariadic
    );
    return null;
  }

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
  private void translateFunction(Stmt source, TruffleString name, List<Expr.Identifier> parameters, Stmt.Block body, boolean isVariadic) {
    boolean startedInLocal = state == ParserState.FUNC_DEF;
    if (startedInLocal && localScope.isDeclared(name)) {
      throw new BladeRuntimeError("'"+ name +"' is already declared in this scope");
    }

    ParserState previousState = state;
    var previousLocalScopes = localScope;

    locals.push(new ArrayList<>());

    this.localScope = new LocalScope(previousLocalScopes, ++scopeDepth);
    this.state = ParserState.FUNC_DEF;

    SourceSection sourceSection = getSourceSection(source);
    b.beginSourceSection(sourceSection.getCharIndex(), sourceSection.getCharLength());
    b.beginRoot();

    b.beginBlock();
    int paramsCount = parameters.size();

    int paramStart = 0;
    if(source instanceof Stmt.Method method) {
      // handle method `self` variable.
      localScope.declare(BString.fromJavaString("self"), locals.peek().size());

      BytecodeLocal argLocal = b.createLocal("self", null);
      locals.peek().add(argLocal);

      b.beginStoreLocal(argLocal);
      beginSourceSection(method);
      b.emitNLoadArgument(0);
      b.endSourceSection();
      b.endStoreLocal();
      paramStart++;
    }

    for (int i = 0; i < paramsCount; i++) {
      Expr.Identifier paramAst = parameters.get(i);
      String paramName = paramAst.token.literal();
      localScope.declare(BString.fromJavaString(paramName), locals.peek().size());

      BytecodeLocal argLocal = b.createLocal(paramName, null);
      locals.peek().add(argLocal);

      b.beginStoreLocal(argLocal);
      beginSourceSection(paramAst);
      b.emitNLoadArgument(i + paramStart);
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

    final int finalParamCount = paramsCount + paramStart;

    NBytecodeRootNode node = b.endRoot();
    node.setParametersCount(finalParamCount);
    node.setName(name);

    b.endSourceSection();

    if(startedInLocal) {
      // FIXME
      throw new BladeRuntimeError("Closures are not yet supported");
    }

    b.beginNSetGlobal(name, false);
    b.emitLoadConstant(new FunctionObj(name, node.getCallTarget(), finalParamCount, isVariadic));
    b.endNSetGlobal();
  }

  private void translateFunction(Stmt source, String name, List<Expr.Identifier> parameters, Stmt.Block body, boolean isVariadic) {
    translateFunction(source, BString.fromJavaString(name), parameters, body, isVariadic);
  }

  private void newLocalScope(Callback callback) {
    ParserState previousState = state;
    var previousLocalScopes = localScope;

    this.localScope = new LocalScope(previousLocalScopes, ++scopeDepth);

    if (state == ParserState.TOP_LEVEL) {
      state = ParserState.NESTED_TOP_LEVEL;
    }

    callback.run();

    state = previousState;
    localScope = previousLocalScopes;
    scopeDepth--;
  }

  private BytecodeLocal getLocal(int index) {
    Object local = locals.peek().get(index);
    if (local instanceof String s) {
      local = b.createLocal(s, null);
      locals.peek().set(index, local);
    }
    return (BytecodeLocal) local;
  }

  private void beginSourceSection(int start, int length) {
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
