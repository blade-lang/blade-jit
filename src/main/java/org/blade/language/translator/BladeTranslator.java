package org.blade.language.translator;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import org.blade.language.nodes.NDynamicObjectRefNode;
import org.blade.language.nodes.NGlobalScopeObjectNode;
import org.blade.language.nodes.NGlobalScopeObjectNodeGen;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.expressions.*;
import org.blade.language.nodes.expressions.arithemetic.*;
import org.blade.language.nodes.expressions.bitwise.*;
import org.blade.language.nodes.expressions.logical.*;
import org.blade.language.nodes.functions.*;
import org.blade.language.nodes.list.NListIndexReadNodeGen;
import org.blade.language.nodes.list.NListIndexWriteNodeGen;
import org.blade.language.nodes.list.NListLiteralNode;
import org.blade.language.nodes.literals.*;
import org.blade.language.nodes.statements.*;
import org.blade.language.nodes.statements.loops.*;
import org.blade.language.nodes.string.NStringLiteralNode;
import org.blade.language.parser.BaseVisitor;
import org.blade.language.parser.Parser;
import org.blade.language.parser.ast.AST;
import org.blade.language.parser.ast.Expr;
import org.blade.language.parser.ast.Stmt;
import org.blade.language.runtime.BladeClass;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.shared.BuiltinClassesModel;
import org.blade.language.shared.LocalRefSlot;

import java.math.BigInteger;
import java.util.*;

public class BladeTranslator extends BaseVisitor<NNode> {
  private final Parser parser;

  private final Shape objectShape;
  private final NGlobalScopeObjectNode globalScopeNode = NGlobalScopeObjectNodeGen.create();
  private FrameDescriptor.Builder frameDescriptor = FrameDescriptor.newBuilder();
  private ParserState state = ParserState.TOP_LEVEL;

  // Trackers
  private int localsCount = 0;
  private Stack<Map<String, NFrameMember>> localScopes = new Stack<>();
  private BladeClass currentClass = null;

  public BladeTranslator(Parser parser, BuiltinClassesModel classesModel) {
    this.parser = parser;
    this.objectShape = classesModel.rootShape;

    // Put the Object class into the local scope so that every class, function, and module
    // is aware that it exists.
    Map<String, NFrameMember> objectClasses = new HashMap<>();
    for (Map.Entry<String, BladeClass> classEntry : classesModel.builtinClasses.entrySet()) {
      objectClasses.put(classEntry.getKey(), new NFrameMember.ClassObject(classEntry.getValue()));
    }
    localScopes.push(objectClasses);
  }

  public NTranslateResult translate(List<Stmt> stmtList) {
    List<NNode> nodes = new ArrayList<>();

    // 1. Add all functions first.
    // This allows using functions in statements (such as other functions)
    // before being defined
    for (Stmt stmt : stmtList) {
      if (stmt instanceof Stmt.Function function) {
        nodes.add(visitFunctionStmt(function));
      }
    }

    // 2. Add everything not a function after
    for (Stmt stmt : stmtList) {
      if (stmt != null && !(stmt instanceof Stmt.Function)) {
        nodes.add(visitStmt(stmt));
      }
    }

    return new NTranslateResult(nodes, frameDescriptor.build());
  }

  @Override
  public NNode visitStmt(Stmt stmt) {
    if (stmt != null) {
      return sourceSection(stmt.accept(this), stmt);
    }

    return null;
  }

  @Override
  public NNode visitExpr(Expr expr) {
    if (expr != null) {
      return sourceSection(expr.accept(this), expr);
    }

    return null;
  }

  @Override
  public NNode visitNilExpr(Expr.Nil expr) {
    return sourceSection(new NNilLiteralNode(), expr);
  }

  @Override
  public NNode visitBooleanExpr(Expr.Boolean expr) {
    return sourceSection(new NBooleanLiteralNode(expr.value), expr);
  }

  @Override
  public NNode visitNumberExpr(Expr.Number expr) {
    String number = expr.token.literal();

    try {
      if (number.startsWith("0x")) {
        return sourceSection(
          new NLongLiteralNode(Long.parseLong(number.substring(2), 16)),
          expr
        );
      } else if (number.startsWith("0b")) {
        return sourceSection(
          new NLongLiteralNode(Long.parseLong(number.substring(2), 2)),
          expr
        );
      } else if (number.startsWith("0c")) {
        return sourceSection(
          new NLongLiteralNode(Long.parseLong(number.substring(2), 8)),
          expr
        );
      }

      return sourceSection(new NLongLiteralNode(Long.parseLong(number)), expr);
    } catch (NumberFormatException e) {
      try {
        // Try to convert it to a big integer.
        return sourceSection(new NBigIntLiteralNode(new BigInteger(number)), expr);
      } catch (NumberFormatException ignored) {
        // it's possible that the long literal is too big to fit in a 32-bit Java `int` -
        // in that case, and it is not a valid big integer as well, fall back to a double literal
        return sourceSection(new NDoubleLiteralNode(Double.parseDouble(number)), expr);
      }
    }
  }

  @Override
  public NNode visitBigNumberExpr(Expr.BigNumber expr) {
    return sourceSection(new NBigIntLiteralNode(new BigInteger(expr.token.literal())), expr);
  }

  @Override
  public NNode visitLiteralExpr(Expr.Literal expr) {
    return sourceSection(new NStringLiteralNode(expr.token.literal()), expr);
  }

  @Override
  public NNode visitIdentifierExpr(Expr.Identifier expr) {
    String id = expr.token.literal();
    NFrameMember member = findFrameMember(id);

    if (member == null || member instanceof NFrameMember.ClassObject) {
      return sourceSection(NGlobalVarRefExprNodeGen.create(globalScopeNode, id), expr);
    } else {
      return sourceSection(
        member instanceof NFrameMember.FunctionArgument argument
          ? new NReadFunctionArgsExprNode(argument.index, id)
          : NLocalRefNodeGen.create(((NFrameMember.LocalVariable) member).index),
        expr);
    }
  }

  @Override
  public NNode visitBinaryExpr(Expr.Binary expr) {
    return sourceSection(switch (expr.op.type()) {
      case PLUS -> NAddNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case MINUS -> NSubtractNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case MULTIPLY -> NMultiplyNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case DIVIDE -> NDivideNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case FLOOR -> NFloorDivideNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case PERCENT -> NModuloNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case POW -> NPowNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case EQUAL_EQ -> NEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case BANG_EQ -> NNotEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case LESS -> NLessThanNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case LESS_EQ -> NLessThanOrEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case GREATER -> NGreaterThanNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case GREATER_EQ -> NGreaterThanOrEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case AMP -> NBitAndNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case BAR -> NBitOrNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case XOR -> NBitXorNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case LSHIFT -> NBitLeftShiftNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case RSHIFT -> NBitRightShiftNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case URSHIFT -> NBitUnsignedRightShiftNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      default -> throw new UnsupportedOperationException(expr.op.literal());
    }, expr);
  }

  @Override
  public NNode visitUnaryExpr(Expr.Unary expr) {
    return sourceSection(switch (expr.op.type()) {
      case MINUS -> NNegateNodeGen.create(visitExpr(expr.right));
      case BANG -> NLogicalNotNodeGen.create(visitExpr(expr.right));
      case TILDE -> NBitNotNodeGen.create(visitExpr(expr.right));
      default -> throw new UnsupportedOperationException(expr.op.literal());
    }, expr);
  }

  @Override
  public NNode visitLogicalExpr(Expr.Logical expr) {
    return sourceSection(switch (expr.op.type()) {
      case EQUAL_EQ -> NEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case BANG_EQ -> NNotEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case LESS -> NLessThanNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case LESS_EQ -> NLessThanOrEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case GREATER -> NGreaterThanNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case GREATER_EQ -> NGreaterThanOrEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case AND -> new NLogicalAndNode(visitExpr(expr.left), visitExpr(expr.right));
      case OR -> new NLogicalOrNode(visitExpr(expr.left), visitExpr(expr.right));
      default -> throw new UnsupportedOperationException(expr.op.literal());
    }, expr);
  }

  @Override
  public NNode visitGroupingExpr(Expr.Grouping expr) {
    return visitExpr(expr.expression);
  }

  @Override
  public NNode visitConditionExpr(Expr.Condition expr) {
    return sourceSection(new NConditionalNode(
      visitExpr(expr.expression),
      visitExpr(expr.truth),
      visitExpr(expr.falsy)
    ), expr);
  }

  @Override
  public NNode visitAssignExpr(Expr.Assign expr) {
    NNode value = visitExpr(expr.value);
    if (expr.expression instanceof Expr.Identifier identifier) {
      String name = identifier.token.literal();

      NFrameMember member = findFrameMember(name);
      if (member == null) {
        return NGlobalAssignExprNodeGen.create(globalScopeNode, value, name);
      } else {
        if (member instanceof NFrameMember.FunctionArgument memberValue) {
          return sourceSection(new NWriteFunctionArgExprNode(value, memberValue.index), expr);
        } else if (member instanceof NFrameMember.ClassObject memberValue) {
          return sourceSection(NGlobalAssignExprNodeGen.create(globalScopeNode, value, memberValue.object.name), expr);
        } else {
          NFrameMember.LocalVariable local = (NFrameMember.LocalVariable) member;
          if (local.constant) {
            throw BladeRuntimeError.error(value, "Assignment to constant variable '", name, "'");
          }

          return sourceSection(NLocalAssignNodeGen.create(value, name, local.index), expr);
        }
      }
    } else if (expr.expression instanceof Expr.Index index) {
      return sourceSection(NListIndexWriteNodeGen.create(
        visitExpr(index.callee),
        visitExpr(index.argument),
        value
      ), expr);
    }

    throw BladeRuntimeError.error(value, "Invalid assignment expression");
  }

  @Override
  public NNode visitNewExpr(Expr.New expr) {
    List<NNode> arguments = new ArrayList<>();
    for (Expr arg : expr.arguments) {
      arguments.add(visitExpr(arg));
    }

    return sourceSection(NNewExprNodeGen.create(visitExpr(expr.expression), arguments), expr);
  }

  @Override
  public NNode visitCallExpr(Expr.Call expr) {
    List<NNode> arguments = new ArrayList<>();
    if (expr.callee instanceof Expr.Identifier) {
      arguments.add(new NNilLiteralNode());
    }

    for (Expr arg : expr.args) {
      arguments.add(visitExpr(arg));
    }

    if (expr.callee instanceof Expr.Identifier || expr.callee instanceof Expr.Anonymous) {
      return sourceSection(NFunctionCallExprNodeGen.create(visitExpr(expr.callee), arguments), expr);
    }
    return sourceSection(new NMethodCallExprNode(visitExpr(expr.callee), arguments), expr);
  }

  @Override
  public NNode visitSetExpr(Expr.Set expr) {
    return sourceSection(NSetPropertyNodeGen.create(
      visitExpr(expr.expression),
      visitExpr(expr.value),
      expr.name.token.literal()
    ), expr);
  }

  @Override
  public NNode visitGetExpr(Expr.Get expr) {
    return sourceSection(NGetPropertyNodeGen.create(visitExpr(expr.expression), expr.name.token.literal()), expr);
  }

  @Override
  public NNode visitIndexExpr(Expr.Index expr) {
    return sourceSection(NListIndexReadNodeGen.create(visitExpr(expr.callee), visitExpr(expr.argument)), expr);
  }

  @Override
  public NNode visitSliceExpr(Expr.Slice expr) {
    NNode callee = visitExpr(expr.callee);
    NNode lower = expr.lower == null ? new NLongLiteralNode(0) : visitExpr(expr.lower);
    NNode upper = expr.upper == null ? new NDoubleLiteralNode(0.0) : visitExpr(expr.upper);
    return sourceSection(NGetSliceNodeGen.create(callee, lower, upper), expr);
  }

  @Override
  public NNode visitArrayExpr(Expr.Array expr) {
    List<NNode> nodes = new ArrayList<>();
    for (Expr e : expr.items) {
      nodes.add(visitExpr(e));
    }
    return sourceSection(new NListLiteralNode(nodes), expr);
  }

  @Override
  public NNode visitDictExpr(Expr.Dict expr) {
    List<NNode> keys = new ArrayList<>();
    List<NNode> values = new ArrayList<>();

    for (Expr e : expr.keys) {
      keys.add(visitExpr(e));
    }

    for (Expr e : expr.values) {
      values.add(visitExpr(e));
    }

//    for (int i = 0; i < expr.keys.size(); i++) {
//      keys.add(visitExpr(expr.keys.get(i)));
//      values.add(visitExpr(expr.values.get(i)));
//    }

    return sourceSection(new NDictionaryLiteralNode(keys, values), expr);
  }

  @Override
  public NNode visitRangeExpr(Expr.Range expr) {
    return NRangeLiteralNodeGen.create(visitExpr(expr.lower), visitExpr(expr.upper));
  }

  @Override
  public NNode visitEchoStmt(Stmt.Echo stmt) {
    return sourceSection(new NEchoStmtNode(visitExpr(stmt.value)), stmt);
  }

  @Override
  public NNode visitExpressionStmt(Stmt.Expression stmt) {
    assert stmt.expression != null;
    return sourceSection(new NExprStmtNode(visitExpr(stmt.expression)), stmt);
  }

  @Override
  public NNode visitVarListStmt(Stmt.VarList varList) {
    List<NNode> nodes = new ArrayList<>();
    for (Stmt stmt : varList.declarations) {
      nodes.add(visitStmt(stmt));
    }

    return sourceSection(new NBlockStmtNode(nodes), varList);
  }

  @Override
  public NNode visitVarStmt(Stmt.Var stmt) {
    boolean isConstant = stmt.isConstant;
    String name = stmt.name.literal();

    NNode value = stmt.value != null ? visitExpr(stmt.value) : sourceSection(new NNilLiteralNode(), stmt);

    if (isConstant && stmt.value == null) {
      throw BladeRuntimeError.error(value, "Constant '", name, "' not initialized");
    }

    if (state != ParserState.TOP_LEVEL) {
      LocalRefSlot slotId = new LocalRefSlot(name, ++localsCount);
      int slot = frameDescriptor.addSlot(FrameSlotKind.Illegal, slotId, isConstant);
      if (localScopes.peek().putIfAbsent(name, new NFrameMember.LocalVariable(slot, isConstant)) != null) {
        throw BladeRuntimeError.error(value, "'", name, "' is already declared in this scope");
      }

      NLocalAssignNode assignment = NLocalAssignNodeGen.create(value, name, slot);
      return sourceSection(new NExprStmtNode(assignment, true), stmt);
    }

    // default to global value
    return sourceSection(NGlobalDeclNodeGen.create(globalScopeNode, value, name, isConstant), stmt);
  }

  @Override
  public NNode visitPropertyStmt(Stmt.Property stmt) {
    return sourceSection(NSetPropertyNodeGen.create(
      new NDynamicObjectRefNode(currentClass),
      stmt.value == null ? new NNilLiteralNode() : sourceSection(visitExpr(stmt.value), stmt.value),
      stmt.name.literal()
    ), stmt);
  }

  @Override
  public NBlockStmtNode visitBlockStmt(Stmt.Block stmt) {
    return (NBlockStmtNode) newLocalScope(() -> {
      List<NNode> nodes = new ArrayList<>();
      for (Stmt statement : stmt.body) {
        if (statement != null) {
          nodes.add(visitStmt(statement));
        }
      }

      return sourceSection(new NBlockStmtNode(nodes), stmt);
    });
  }

  @Override
  public NNode visitIfStmt(Stmt.If stmt) {
    return sourceSection(new NIfStmtNode(
      visitExpr(stmt.condition),
      visitStmt(stmt.thenBranch),
      visitStmt(stmt.elseBranch)
    ), stmt);
  }

  @Override
  public NNode visitBreakStmt(Stmt.Break stmt) {
    return sourceSection(new NBreakNode(), stmt);
  }

  @Override
  public NNode visitContinueStmt(Stmt.Continue stmt) {
    return sourceSection(new NContinueNode(), stmt);
  }

  @Override
  public NNode visitWhileStmt(Stmt.While stmt) {
    return sourceSection(new NWhileStmtNode(
      visitExpr(stmt.condition),
      visitStmt(stmt.body)
    ), stmt);
  }

  @Override
  public NNode visitDoWhileStmt(Stmt.DoWhile stmt) {
    return sourceSection(new NDoWhileStmtNode(
      visitExpr(stmt.condition),
      visitStmt(stmt.body)
    ), stmt);
  }

  @Override
  public NNode visitIterStmt(Stmt.Iter stmt) {
    return newLocalScope(() -> sourceSection(new NIterStmtNode(
      stmt.declaration != null ? visitStmt(stmt.declaration) : null,
      stmt.condition != null ? visitExpr(stmt.condition) : null,
      stmt.interation != null ? visitExpressionStmt(stmt.interation) : null,
      visitStmt(stmt.body)
    ), stmt));
  }

  @Override
  public NNode visitFunctionStmt(Stmt.Function stmt) {
    return translateFunction(
      stmt,
      stmt.name.literal(),
      stmt.parameters,
      stmt.body,
      globalScopeNode,
      stmt.isVariadic
    );
  }

  @Override
  public NNode visitMethodStmt(Stmt.Method stmt) {
    return translateFunction(
      stmt,
      stmt.name.literal(),
      stmt.parameters,
      stmt.body,
      globalScopeNode,
      stmt.isVariadic
    );
  }

  @Override
  public NNode visitReturnStmt(Stmt.Return stmt) {
    NNode value = stmt.value == null ?
      new NNilLiteralNode() :
      visitExpr(stmt.value);

    if (state != ParserState.FUNC_DEF) {
      throw BladeRuntimeError.error(value, "`return` keyword is not allowed in this scope");
    }

    return sourceSection(new NReturnStmtNode(value), stmt);
  }

  @Override
  public NNode visitClassStmt(Stmt.Class stmt) {
    if (state == ParserState.FUNC_DEF) {
      throw BladeRuntimeError.create("Classes cannot be nested in functions");
    }

    String className = stmt.name.literal();
    String superClass = stmt.superclass != null ?
      stmt.superclass.token.literal() :
      "Object";

    BladeClass classObject;
    NFrameMember frameMember = localScopes.getFirst().get(superClass);
    if (frameMember instanceof NFrameMember.ClassObject classMember) {
      BladeClass superClassObject = classMember.object;
      classObject = new BladeClass(objectShape, className, superClassObject);
    } else {
      throw BladeRuntimeError.create("Class '", className, "' extends unknown or frozen class '", superClass, "'");
    }

    localScopes.getFirst().put(className, new NFrameMember.ClassObject(classObject));

    List<NNode> methods = new ArrayList<>();
    List<NNode> properties = new ArrayList<>();
    List<NNode> operators = new ArrayList<>();

    // set current class
    currentClass = classObject;

    for (Stmt.Property property : stmt.properties) {
      properties.add(visitPropertyStmt(property));
    }

    for (Stmt.Method method : stmt.methods) {
      methods.add(translateFunction(
        method,
        method.name.literal(),
        method.parameters,
        method.body,
        new NDynamicObjectRefNode(classObject),
        method.isVariadic
      ));
    }

    for (Stmt.Method method : stmt.operators) {
      methods.add(translateFunction(
        method,
        method.name.literal(),
        method.parameters,
        method.body,
        new NDynamicObjectRefNode(classObject),
        method.isVariadic
      ));
    }

    // reset current class
    currentClass = null;

    // deliberately not wrapped in sourceSection so that debuggers won't stop
    // in class declarations and their global variable
    return sourceSection(NGlobalDeclNodeGen.create(
      NGlobalScopeObjectNodeGen.create(),
      sourceSection(new NClassDeclNode(methods, properties, operators, classObject), stmt),
      className,
      false
    ), stmt);
  }

  @Override
  public NNode visitSelfExpr(Expr.Self expr) {
    if (currentClass == null) {
      throw BladeRuntimeError.create("`self` keyword not allowed outside a class");
    }

    return sourceSection(new NSelfLiteralNode(), expr);
  }

  @Override
  public NNode visitParentExpr(Expr.Parent expr) {
    if (currentClass == null) {
      throw BladeRuntimeError.create("`parent` keyword not allowed outside a class");
    }

    return sourceSection(new NParentExprNode(currentClass), expr);
  }

  @Override
  public NNode visitRaiseStmt(Stmt.Raise stmt) {
    return sourceSection(NRaiseStmtNodeGen.create(visitExpr(stmt.exception), false), stmt);
  }

  @Override
  public NNode visitAssertStmt(Stmt.Assert stmt) {
    return sourceSection(new NAssertStmtNode(
      sourceSection(visitExpr(stmt.expression), stmt.expression),
      sourceSection(NRaiseStmtNodeGen.create(
        stmt.message == null || stmt.message instanceof Expr.Literal ?
          sourceSection(NNewExprNodeGen.create(
            NGlobalVarRefExprNodeGen.create(globalScopeNode, "AssertError"),
            List.of(new NStringLiteralNode(stmt.message instanceof Expr.Literal literal ?
              literal.token.literal() :
              "Failed assertion"
            ))
          ), stmt) :
          visitExpr(stmt.message),
        true
      ), stmt.message == null ? stmt : stmt.message)
    ), stmt);
  }

  @Override
  public NNode visitCatchStmt(Stmt.Catch stmt) {
    NNode body = visitBlockStmt(stmt.body);
    NNode thenBody = stmt.finallyBody == null ? null : visitBlockStmt(stmt.finallyBody);
    NNode asBody = null;
    int slot = -1;

    if (stmt.name != null) {

      String errorName = stmt.name.token.literal();
      LocalRefSlot slotId = new LocalRefSlot(errorName, ++localsCount);
      slot = frameDescriptor.addSlot(FrameSlotKind.Object, slotId, 1);
      if (localScopes.peek().putIfAbsent(errorName, new NFrameMember.LocalVariable(slot, true)) != null) {
        throw BladeRuntimeError.error(thenBody, "'", errorName, "' is already declared in this scope");
      }

      // parse the 'catch' statement block
      asBody = visitBlockStmt(stmt.catchBody);
    }

    return new NTryCatchStmtNode(body, slot, asBody, thenBody);
  }

  @Override
  public NNode visitAnonymousExpr(Expr.Anonymous expr) {
    return new NAnonymousExprNode(
      translateFunction(
        expr.function,
        "@anonymous",
        expr.function.parameters,
        expr.function.body,
        globalScopeNode,
        expr.function.isVariadic
      )
    );
  }

  private NNode translateFunction(Stmt source, String name, List<Expr.Identifier> parameters, Stmt.Block body, NNode root, boolean isVariadic) {
    FrameDescriptor.Builder previousFrameDescriptor = frameDescriptor;
    ParserState previousState = state;
    var previousLocalScopes = localScopes;

    this.frameDescriptor = FrameDescriptor.newBuilder();
    this.state = ParserState.FUNC_DEF;
    this.localScopes = new Stack<>();

    Map<String, NFrameMember> localVariables = new HashMap<>();
    for (int i = 0; i < parameters.size(); i++) {
      localVariables.put(parameters.get(i).token.literal(), new NFrameMember.FunctionArgument(i + 1));
    }
    this.localScopes.push(localVariables);

    NBlockStmtNode statements = visitBlockStmt(body);

    FrameDescriptor frameDescriptor = this.frameDescriptor.build();
    this.frameDescriptor = previousFrameDescriptor;
    this.state = previousState;
    this.localScopes = previousLocalScopes;

    return sourceSection(NFunctionStmtNodeGen.create(
      root,
      name,
      frameDescriptor,
      (NFunctionBodyNode) sourceSection(new NFunctionBodyNode(statements), body),
      parameters.size(),
      isVariadic ? 1 : 0
    ), source);
  }

  private NFrameMember findFrameMember(String name) {
    for (Map<String, NFrameMember> scope : localScopes) {
      NFrameMember member = scope.get(name);
      if (member != null) {
        return member;
      }
    }

    return null;
  }

  private NNode newLocalScope(Callback callback) {
    ParserState previousState = state;
    if (state == ParserState.TOP_LEVEL) {
      state = ParserState.NESTED_TOP_LEVEL;
    }
    localScopes.push(new HashMap<>());

    NNode result = callback.run();

    state = previousState;
    localScopes.pop();
    return result;
  }

  private boolean isReadingExpr(Expr expr) {
    return expr instanceof Expr.Identifier;
  }

  private NNode sourceSection(NNode node, Object object) {
    if (object instanceof AST ast) {
//      System.out.println("SL = " +ast.startLine+", EL = " +ast.endLine+", SC = " + ast.startColumn + ", EC = " +ast.endColumn);
      return node.setSourceSection(parser.lexer.source.createSection(
        ast.startLine, ast.startColumn + 1,
        ast.endLine, ast.endColumn + 1
      ));
    }

//    System.out.println(node);
//    System.out.println(object);
    return node.setSourceSection(parser.lexer.source.createSection(1));
  }

  public SourceSection getRootSourceSection() {
    return parser.lexer.source.createSection(0, parser.lexer.source.getLength());
  }

  // State management
  private enum ParserState {TOP_LEVEL, NESTED_TOP_LEVEL, FUNC_DEF}

  interface Callback {
    NNode run();
  }
}
