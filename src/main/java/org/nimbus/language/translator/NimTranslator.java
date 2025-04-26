package org.nimbus.language.translator;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import org.nimbus.language.nodes.NDynamicObjectRefNode;
import org.nimbus.language.nodes.NGlobalScopeObjectNode;
import org.nimbus.language.nodes.NGlobalScopeObjectNodeGen;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.expressions.*;
import org.nimbus.language.nodes.expressions.arithemetic.*;
import org.nimbus.language.nodes.expressions.bitwise.*;
import org.nimbus.language.nodes.expressions.logical.*;
import org.nimbus.language.nodes.functions.*;
import org.nimbus.language.nodes.list.NListIndexReadNodeGen;
import org.nimbus.language.nodes.list.NListIndexWriteNodeGen;
import org.nimbus.language.nodes.list.NListLiteralNode;
import org.nimbus.language.nodes.literals.*;
import org.nimbus.language.nodes.statements.*;
import org.nimbus.language.nodes.statements.loops.*;
import org.nimbus.language.nodes.string.NStringLiteralNode;
import org.nimbus.language.parser.BaseVisitor;
import org.nimbus.language.parser.Parser;
import org.nimbus.language.parser.ast.AST;
import org.nimbus.language.parser.ast.Expr;
import org.nimbus.language.parser.ast.Stmt;
import org.nimbus.language.runtime.NimClass;
import org.nimbus.language.runtime.NimRuntimeError;
import org.nimbus.language.shared.NBuiltinClassesModel;
import org.nimbus.language.shared.NLocalRefSlot;

import java.util.*;

public class NimTranslator extends BaseVisitor<NNode> {
  private final Parser parser;

  private final Shape objectShape;
  private final NGlobalScopeObjectNode globalScopeNode = NGlobalScopeObjectNodeGen.create();
  private FrameDescriptor.Builder frameDescriptor = FrameDescriptor.newBuilder();
  private ParserState state = ParserState.TOP_LEVEL;

  // Trackers
  private int localsCount = 0;
  private Stack<Map<String, NFrameMember>> localScopes = new Stack<>();
  private NimClass currentClass = null;

  public NimTranslator(Parser parser, NBuiltinClassesModel classesModel) {
    this.parser = parser;
    this.objectShape = classesModel.rootShape;

    // Put the Object class into the local scope so that every class, function, and module
    // is aware that it exists.
    Map<String, NFrameMember> objectClasses = new HashMap<>();
    for (Map.Entry<String, NimClass> classEntry : classesModel.builtinClasses.entrySet()) {
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
      return sourceSection(new NLongLiteralNode(Integer.parseInt(number)), expr);
    } catch (NumberFormatException e) {
      // it's possible that the integer literal is too big to fit in a 32-bit Java `int` -
      // in that case, fall back to a double literal
      return sourceSection(new NDoubleLiteralNode(Double.parseDouble(number)), expr);
    }
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
          ? new NReadFunctionArgsExprNode(argument.index)
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
    if (expr.expression instanceof Expr.Identifier identifier) {
      String name = identifier.token.literal();
      NNode value = visitExpr(expr.value);

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
            throw NimRuntimeError.create("Assignment to constant variable '", name, "'");
          }

          return sourceSection(NLocalAssignNodeGen.create(value, local.index), expr);
        }
      }
    } else if (expr.expression instanceof Expr.Index index) {
      if (index.arguments.size() == 1) {
        return sourceSection(NListIndexWriteNodeGen.create(
          visitExpr(index.callee),
          visitExpr(index.arguments.getFirst()),
          visitExpr(expr.value)
        ), expr);
      }
    }

    throw NimRuntimeError.create("Invalid assignment expression");
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

    if (expr.callee instanceof Expr.Identifier) {
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
    if (expr.arguments.size() == 1) {
      return sourceSection(NListIndexReadNodeGen.create(visitExpr(expr.callee), visitExpr(expr.arguments.getFirst())), expr);
    }
    throw NimRuntimeError.create("Slices are not yet supported");
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

    if (isConstant && stmt.value == null) {
      throw NimRuntimeError.create("Constant '", name, "' not initialized");
    }

    NNode value = stmt.value != null ? visitExpr(stmt.value) : new NNilLiteralNode();

    if (state != ParserState.TOP_LEVEL) {
      NLocalRefSlot slotId = new NLocalRefSlot(name, ++localsCount);
      int slot = frameDescriptor.addSlot(FrameSlotKind.Illegal, slotId, isConstant);
      if (localScopes.peek().putIfAbsent(name, new NFrameMember.LocalVariable(slot, isConstant)) != null) {
        throw NimRuntimeError.create("'", name, "' is already declared in this scope");
      }

      NLocalAssignNode assignment = NLocalAssignNodeGen.create(value, slot);
      return sourceSection(new NExprStmtNode(assignment, true), stmt);
    }

    // default to global value
    return sourceSection(NGlobalDeclNodeGen.create(globalScopeNode, value, name, isConstant), stmt);
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
    if (state != ParserState.FUNC_DEF) {
      throw NimRuntimeError.create("`return` keyword is not allowed in this scope");
    }

    return sourceSection(new NReturnStmtNode(
      stmt.value == null ?
        new NNilLiteralNode() :
        visitExpr(stmt.value)
    ), stmt);
  }

  @Override
  public NNode visitClassStmt(Stmt.Class stmt) {
    if (state == ParserState.FUNC_DEF) {
      throw NimRuntimeError.create("Classes cannot be nested in functions");
    }

    String className = stmt.name.literal();
    String superClass = stmt.superclass != null ?
      stmt.superclass.token.literal() :
      "Object";

    NimClass classObject;
    NFrameMember frameMember = localScopes.getFirst().get(superClass);
    if (frameMember instanceof NFrameMember.ClassObject classMember) {
      NimClass superClassObject = classMember.object;
      classObject = new NimClass(objectShape, className, superClassObject);
    } else {
      throw NimRuntimeError.create("Class '", className, "' extends unknown class '", superClass, "'");
    }

    localScopes.getFirst().put(className, new NFrameMember.ClassObject(classObject));

    // set current class
    currentClass = classObject;

    List<NNode> methods = new ArrayList<>();

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

    // reset current class
    currentClass = null;

    return NGlobalDeclNodeGen.create(
      NGlobalScopeObjectNodeGen.create(),
      new NClassDeclNode(methods, classObject),
      className,
      false
    );
  }

  @Override
  public NNode visitSelfExpr(Expr.Self expr) {
    if(currentClass == null) {
      throw NimRuntimeError.create("`self` keyword not allowed outside a class");
    }

    return new NSelfLiteralNode();
  }

  @Override
  public NNode visitParentExpr(Expr.Parent expr) {
    if(currentClass == null) {
      throw NimRuntimeError.create("`parent` keyword not allowed outside a class");
    }

    return new NParentExprNode(currentClass);
  }

  @Override
  public NNode visitRaiseStmt(Stmt.Raise stmt) {
    return sourceSection(NRaiseStmtNodeGen.create(visitExpr(stmt.exception)), stmt);
  }

  @Override
  public NNode visitCatchStmt(Stmt.Catch stmt) {
    NNode body = visitBlockStmt(stmt.body);
    NNode thenBody = stmt.thenBody == null ? null : visitBlockStmt(stmt.thenBody);
    NNode asBody = null;
    int slot = -1;

    if(stmt.name != null) {
      String errorName = stmt.name.token.literal();
      NLocalRefSlot slotId = new NLocalRefSlot(errorName, ++localsCount);
      slot = frameDescriptor.addSlot(FrameSlotKind.Object, slotId, 1);
      if (localScopes.peek().putIfAbsent(errorName, new NFrameMember.LocalVariable(slot, true)) != null) {
        throw NimRuntimeError.create("'", errorName, "' is already declared in this scope");
      }

      // parse the 'catch' statement block
      asBody = visitBlockStmt(stmt.asBody);
    }

    return new NCatchStmtNode(body, slot, asBody, thenBody);
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
      new NFunctionBodyNode(statements),
      parameters.size(),
      isVariadic ? 1 : 0
    ), source);
  }

  private NFrameMember findFrameMember(String name) {
    for (var scope : localScopes) {
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

  // State management
  private enum ParserState {TOP_LEVEL, NESTED_TOP_LEVEL, FUNC_DEF}

  private NNode sourceSection(NNode node, Object object) {
    if(object instanceof AST ast) {
//      System.out.println("SL = " +ast.startLine+", EL = " +ast.endLine+", SC = " + ast.startColumn + ", EC = " +ast.endColumn);
      return node.setSourceSection(parser.lexer.source.createSection(
        ast.startLine, ast.startColumn + 1,
        ast.endLine, ast.endColumn + 1
      ));
    }

    return node.setSourceSection(parser.lexer.source.createSection(1));
  }

  interface Callback {
    NNode run();
  }
}
