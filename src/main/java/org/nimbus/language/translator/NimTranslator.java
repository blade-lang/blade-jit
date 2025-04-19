package org.nimbus.language.translator;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.nodes.NDynamicObjectRefNode;
import org.nimbus.language.nodes.NGlobalScopeObjectNode;
import org.nimbus.language.nodes.NGlobalScopeObjectNodeGen;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.functions.*;
import org.nimbus.language.nodes.expressions.*;
import org.nimbus.language.nodes.expressions.arithemetic.*;
import org.nimbus.language.nodes.expressions.bitwise.*;
import org.nimbus.language.nodes.list.NListIndexReadNodeGen;
import org.nimbus.language.nodes.list.NListIndexWriteNodeGen;
import org.nimbus.language.nodes.list.NListLiteralNode;
import org.nimbus.language.nodes.literals.*;
import org.nimbus.language.nodes.expressions.logical.*;
import org.nimbus.language.nodes.statements.*;
import org.nimbus.language.nodes.statements.loops.*;
import org.nimbus.language.nodes.string.NStringLiteralNode;
import org.nimbus.language.parser.BaseVisitor;
import org.nimbus.language.parser.ast.Expr;
import org.nimbus.language.parser.ast.Stmt;
import org.nimbus.language.runtime.NimClass;
import org.nimbus.language.shared.NLocalRefSlot;
import org.nimbus.language.runtime.NimRuntimeError;

import java.util.*;

public class NimTranslator extends BaseVisitor<NNode> {
  private final Shape listShape;
  private final Shape objectShape;

  private enum ParserState { TOP_LEVEL, NESTED_TOP_LEVEL, FUNC_DEF }

  private FrameDescriptor.Builder frameDescriptor = FrameDescriptor.newBuilder();
  private Stack<Map<String, NFrameMember>> localScopes = new Stack<>();
  private ParserState state = ParserState.TOP_LEVEL;
  private int localsCount = 0;

  private final NGlobalScopeObjectNode globalScopeNode = NGlobalScopeObjectNodeGen.create();

  public NimTranslator(Shape objectShape, Shape listShape) {
    this.objectShape = objectShape;
    this.listShape = listShape;
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
    if(stmt != null) {
      return stmt.accept(this);
    }

    return null;
  }

  @Override
  public NNode visitExpr(Expr expr) {
    if(expr != null) {
      return expr.accept(this);
    }

    return null;
  }

  @Override
  public NNode visitNilExpr(Expr.Nil expr) {
    return new NNilLiteralNode();
  }

  @Override
  public NNode visitBooleanExpr(Expr.Boolean expr) {
    return new NBooleanLiteralNode(expr.value);
  }

  @Override
  public NNode visitNumberExpr(Expr.Number expr) {
    String number = expr.token.literal();

    try {
      return new NLongLiteralNode(Integer.parseInt(number));
    } catch (NumberFormatException e) {
      // it's possible that the integer literal is too big to fit in a 32-bit Java `int` -
      // in that case, fall back to a double literal
      return new NDoubleLiteralNode(Double.parseDouble(number));
    }
  }

  @Override
  public NNode visitLiteralExpr(Expr.Literal expr) {
    return new NStringLiteralNode(expr.token.literal());
  }

  @Override
  public NNode visitIdentifierExpr(Expr.Identifier expr) {
    String id = expr.token.literal();
    NFrameMember member = findFrameMember(id);

    if (member == null) {
      return NGlobalVarRefExprNodeGen.create(globalScopeNode, id);
    } else {
      return member instanceof NFrameMember.FunctionArgument argument
        ? new NReadFunctionArgsExprNode(argument.index)
        : NLocalRefNodeGen.create(((NFrameMember.LocalVariable) member).index);
    }
  }

  @Override
  public NNode visitBinaryExpr(Expr.Binary expr) {
    return switch (expr.op.type()) {
      case PLUS -> NAddNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case MINUS -> NSubtractNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case MULTIPLY -> NMultiplyNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case DIVIDE -> NDivideNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case FLOOR -> NFloorDivideNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
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
    };
  }

  @Override
  public NNode visitUnaryExpr(Expr.Unary expr) {
    return switch (expr.op.type()) {
      case MINUS -> NNegateNodeGen.create(visitExpr(expr.right));
      case BANG -> new NLogicalNotNode(visitExpr(expr.right));
      case TILDE -> NBitNotNodeGen.create(visitExpr(expr.right));
      default -> throw new UnsupportedOperationException(expr.op.literal());
    };
  }

  @Override
  public NNode visitLogicalExpr(Expr.Logical expr) {
    return switch (expr.op.type()) {
      case EQUAL_EQ -> NEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case BANG_EQ -> NNotEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case LESS -> NLessThanNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case LESS_EQ -> NLessThanOrEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case GREATER -> NGreaterThanNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case GREATER_EQ -> NGreaterThanOrEqualNodeGen.create(visitExpr(expr.left), visitExpr(expr.right));
      case AND -> new NLogicalAndNode(visitExpr(expr.left), visitExpr(expr.right));
      case OR -> new NLogicalOrNode(visitExpr(expr.left), visitExpr(expr.right));
      default -> throw new UnsupportedOperationException(expr.op.literal());
    };
  }

  @Override
  public NNode visitGroupingExpr(Expr.Grouping expr) {
    return visitExpr(expr.expression);
  }

  @Override
  public NNode visitConditionExpr(Expr.Condition expr) {
    return new NConditionalNode(
      visitExpr(expr.expression),
      visitExpr(expr.truth),
      visitExpr(expr.falsy)
    );
  }

  @Override
  public NNode visitAssignExpr(Expr.Assign expr) {
    if (expr.expression instanceof Expr.Identifier identifier) {
      String name = identifier.token.literal();
      NNode value = visitExpr(expr.value);

      NFrameMember member = findFrameMember(name);
      if(member == null) {
        return NGlobalAssignExprNodeGen.create(globalScopeNode, value, name);
      } else {
        if(member instanceof NFrameMember.FunctionArgument memberValue) {
          return new NWriteFunctionArgExprNode(value, memberValue.index);
        } else {
          NFrameMember.LocalVariable local = (NFrameMember.LocalVariable) member;
          if (local.constant) {
            throw new NimRuntimeError("assignment to constant variable '" + name + "'");
          }

          return NLocalAssignNodeGen.create(value, local.index);
        }
      }
    } else if(expr.expression instanceof Expr.Index index) {
      if(index.arguments.size() == 1) {
        return NListIndexWriteNodeGen.create(
          visitExpr(index.callee),
          visitExpr(index.arguments.getFirst()),
          visitExpr(expr.value)
        );
      }
    }

    throw new NimRuntimeError("invalid assignment expression");
  }

  @Override
  public NNode visitNewExpr(Expr.New expr) {
    List<NNode> arguments = new ArrayList<>();
    for(Expr arg : expr.arguments) {
      arguments.add(visitExpr(arg));
    }

    return NNewExprNodeGen.create(visitExpr(expr.expression), arguments);
  }

  @Override
  public NNode visitCallExpr(Expr.Call expr) {
    List<NNode> arguments = new ArrayList<>();
    if(expr.callee instanceof Expr.Identifier) {
      arguments.add(new NNilLiteralNode());
    }

    for (Expr arg : expr.args) {
      arguments.add(visitExpr(arg));
    }

    if(expr.callee instanceof Expr.Identifier) {
      return NFunctionCallExprNodeGen.create(visitExpr(expr.callee), arguments);
    }
    return new NMethodCallExprNode(visitExpr(expr.callee), arguments);
  }

  @Override
  public NNode visitSetExpr(Expr.Set expr) {
    return NSetPropertyNodeGen.create(
      visitExpr(expr.expression),
      visitExpr(expr.value),
      expr.name.token.literal()
    );
  }

  @Override
  public NNode visitGetExpr(Expr.Get expr) {
    return NGetPropertyNodeGen.create(visitExpr(expr.expression), expr.name.token.literal());
  }

  @Override
  public NNode visitIndexExpr(Expr.Index expr) {
    if(expr.arguments.size() == 1) {
      return NListIndexReadNodeGen.create(visitExpr(expr.callee), visitExpr(expr.arguments.getFirst()));
    }
    throw new NimRuntimeError("slices are not yet supported");
  }

  @Override
  public NNode visitArrayExpr(Expr.Array expr) {
    List<NNode> nodes = new ArrayList<>();
    for(Expr e : expr.items) {
      nodes.add(visitExpr(e));
    }
    return new NListLiteralNode(nodes);
  }

  @Override
  public NNode visitEchoStmt(Stmt.Echo stmt) {
    return new NEchoStmtNode(visitExpr(stmt.value));
  }

  @Override
  public NNode visitExpressionStmt(Stmt.Expression stmt) {
    assert stmt.expression != null;
    return visitExpr(stmt.expression);
  }

  @Override
  public NNode visitVarListStmt(Stmt.VarList varList) {
    List<NNode> nodes = new ArrayList<>();
    for (Stmt stmt : varList.declarations) {
      nodes.add(visitStmt(stmt));
    }

    return new NBlockStmtNode(nodes);
  }

  @Override
  public NNode visitVarStmt(Stmt.Var stmt) {
    boolean isConstant = stmt.isConstant;
    String name = stmt.name.literal();

    if (isConstant && stmt.value == null) {
      throw new NimRuntimeError("constant '" + name + "' not initialized");
    }

    NNode value = stmt.value != null ? visitExpr(stmt.value) : new NNilLiteralNode();

    if (state != ParserState.TOP_LEVEL) {
      NLocalRefSlot slotId = new NLocalRefSlot(name, ++localsCount);
      int slot = frameDescriptor.addSlot(FrameSlotKind.Illegal, slotId, isConstant);
      if (localScopes.peek().putIfAbsent(name, new NFrameMember.LocalVariable(slot, isConstant)) != null) {
        throw new NimRuntimeError("'" + name + "' already declared in this scope");
      }

      NLocalAssignNode assignment = NLocalAssignNodeGen.create(value, slot);
      return new NExprStmtNode(assignment, true);
    }

    // default to global value
    return NGlobalDeclNodeGen.create(globalScopeNode, value, name, isConstant);
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

      return new NBlockStmtNode(nodes);
    });
  }

  @Override
  public NNode visitIfStmt(Stmt.If stmt) {
    return new NIfStmtNode(
      visitExpr(stmt.condition),
      visitStmt(stmt.thenBranch),
      visitStmt(stmt.elseBranch)
    );
  }

  @Override
  public NNode visitBreakStmt(Stmt.Break stmt) {
    return new NBreakNode();
  }

  @Override
  public NNode visitContinueStmt(Stmt.Continue stmt) {
    return new NContinueNode();
  }

  @Override
  public NNode visitWhileStmt(Stmt.While stmt) {
    return new NWhileStmtNode(
      visitExpr(stmt.condition),
      visitStmt(stmt.body)
    );
  }

  @Override
  public NNode visitDoWhileStmt(Stmt.DoWhile stmt) {
    return new NDoWhileStmtNode(
      visitExpr(stmt.condition),
      visitStmt(stmt.body)
    );
  }

  @Override
  public NNode visitIterStmt(Stmt.Iter stmt) {
    return newLocalScope(() -> new NIterStmtNode(
      stmt.declaration != null ? visitStmt(stmt.declaration) : null,
      stmt.condition != null ? visitExpr(stmt.condition) : null,
      stmt.interation != null ? visitExpressionStmt(stmt.interation) : null,
      visitStmt(stmt.body)
    ));
  }

  @Override
  public NNode visitFunctionStmt(Stmt.Function stmt) {
    return translateFunction(
      stmt.name.literal(),
      stmt.parameters,
      stmt.body,
      globalScopeNode
    );
  }

  @Override
  public NNode visitMethodStmt(Stmt.Method stmt) {
    return translateFunction(
      stmt.name.literal(),
      stmt.parameters,
      stmt.body,
      globalScopeNode
    );
  }

  @Override
  public NNode visitReturnStmt(Stmt.Return stmt) {
    if (state != ParserState.FUNC_DEF) {
      throw new NimRuntimeError("return is not allowed in this scope");
    }

    return new NReturnStmtNode(
      stmt.value == null ?
        new NNilLiteralNode() :
      visitExpr(stmt.value)
    );
  }

  @Override
  public NNode visitClassStmt(Stmt.Class stmt) {
    if(state == ParserState.FUNC_DEF) {
      throw new NimRuntimeError("Not yet supported!");
    }

    List<NNode> methods = new ArrayList<>();
    NimClass classPrototype = new NimClass(objectShape, stmt.name.literal());

    for(Stmt.Method method : stmt.methods) {
      methods.add(translateFunction(
        method.name.literal(),
        method.parameters,
        method.body,
        new NDynamicObjectRefNode(classPrototype)
      ));
    }

    return NGlobalDeclNodeGen.create(
      NGlobalScopeObjectNodeGen.create(),
      new NClassDeclNode(methods, classPrototype),
      stmt.name.literal(),
      false
    );
  }

  @Override
  public NNode visitSelfExpr(Expr.Self expr) {
    return new NSelfLiteralNode();
  }

  private NNode translateFunction(String name, List<Expr. Identifier> parameters, Stmt. Block body, NNode root) {
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

    return NFunctionStmtNodeGen.create(
      root,
      name,
      frameDescriptor,
      new NFunctionBodyNode(statements),
      parameters.size()
    );
  }

  private NFrameMember findFrameMember(String name) {
    for(var scope : localScopes) {
      NFrameMember member = scope.get(name);
      if(member != null) {
        return member;
      }
    }

    return null;
  }

  private NNode newLocalScope(Callback callback) {
    ParserState previousState = state;
    if(state == ParserState.TOP_LEVEL) {
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

  interface Callback {
    NNode run();
  }
}
