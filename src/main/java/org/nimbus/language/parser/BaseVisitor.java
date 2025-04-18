package org.nimbus.language.parser;

import org.nimbus.language.parser.ast.Expr;
import org.nimbus.language.parser.ast.Stmt;

public class BaseVisitor<T> implements Expr.Visitor<T>, Stmt.Visitor<T> {
  @Override
  public T visitNilExpr(Expr.Nil expr) {
    return null;
  }

  @Override
  public T visitBooleanExpr(Expr.Boolean expr) {
    return null;
  }

  @Override
  public T visitNumberExpr(Expr.Number expr) {
    return null;
  }

  @Override
  public T visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public T visitUnaryExpr(Expr.Unary expr) {
    return null;
  }

  @Override
  public T visitBinaryExpr(Expr.Binary expr) {
    return null;
  }

  @Override
  public T visitLogicalExpr(Expr.Logical expr) {
    return null;
  }

  @Override
  public T visitRangeExpr(Expr.Range expr) {
    return null;
  }

  @Override
  public T visitGroupingExpr(Expr.Grouping expr) {
    return null;
  }

  @Override
  public T visitIdentifierExpr(Expr.Identifier expr) {
    return null;
  }

  @Override
  public T visitConditionExpr(Expr.Condition expr) {
    return null;
  }

  @Override
  public T visitCallExpr(Expr.Call expr) {
    return null;
  }

  @Override
  public T visitGetExpr(Expr.Get expr) {
    return null;
  }

  @Override
  public T visitSetExpr(Expr.Set expr) {
    return null;
  }

  @Override
  public T visitIndexExpr(Expr.Index expr) {
    return null;
  }

  @Override
  public T visitArrayExpr(Expr.Array expr) {
    return null;
  }

  @Override
  public T visitDictExpr(Expr.Dict expr) {
    return null;
  }

  @Override
  public T visitNewExpr(Expr.New expr) {
    return null;
  }

  @Override
  public T visitParentExpr(Expr.Parent expr) {
    return null;
  }

  @Override
  public T visitSelfExpr(Expr.Self expr) {
    return null;
  }

  @Override
  public T visitAssignExpr(Expr.Assign expr) {
    return null;
  }

  @Override
  public T visitAnonymousExpr(Expr.Anonymous expr) {
    return null;
  }

  @Override
  public T visitExpr(Expr expr) {
    return null;
  }

  @Override
  public T visitEchoStmt(Stmt.Echo stmt) {
    return null;
  }

  @Override
  public T visitExpressionStmt(Stmt.Expression stmt) {
    return null;
  }

  @Override
  public T visitIfStmt(Stmt.If stmt) {
    return null;
  }

  @Override
  public T visitIterStmt(Stmt.Iter stmt) {
    return null;
  }

  @Override
  public T visitWhileStmt(Stmt.While stmt) {
    return null;
  }

  @Override
  public T visitDoWhileStmt(Stmt.DoWhile stmt) {
    return null;
  }

  @Override
  public T visitForStmt(Stmt.For stmt) {
    return null;
  }

  @Override
  public T visitContinueStmt(Stmt.Continue stmt) {
    return null;
  }

  @Override
  public T visitBreakStmt(Stmt.Break stmt) {
    return null;
  }

  @Override
  public T visitRaiseStmt(Stmt.Raise stmt) {
    return null;
  }

  @Override
  public T visitReturnStmt(Stmt.Return stmt) {
    return null;
  }

  @Override
  public T visitAssertStmt(Stmt.Assert stmt) {
    return null;
  }

  @Override
  public T visitUsingStmt(Stmt.Using stmt) {
    return null;
  }

  @Override
  public T visitImportStmt(Stmt.Import stmt) {
    return null;
  }

  @Override
  public T visitCatchStmt(Stmt.Catch stmt) {
    return null;
  }

  @Override
  public T visitBlockStmt(Stmt.Block stmt) {
    return null;
  }

  @Override
  public T visitAssignStmt(Stmt.Assign stmt) {
    return null;
  }

  @Override
  public T visitVarStmt(Stmt.Var stmt) {
    return null;
  }

  @Override
  public T visitFunctionStmt(Stmt.Function stmt) {
    return null;
  }

  @Override
  public T visitMethodStmt(Stmt.Method stmt) {
    return null;
  }

  @Override
  public T visitPropertyStmt(Stmt.Property stmt) {
    return null;
  }

  @Override
  public T visitClassStmt(Stmt.Class stmt) {
    return null;
  }

  @Override
  public T visitVarListStmt(Stmt.VarList stmt) {
    return null;
  }

  @Override
  public T visitStmt(Stmt stmt) {
    return null;
  }
}
