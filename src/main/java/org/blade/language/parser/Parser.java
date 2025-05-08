package org.blade.language.parser;

import org.blade.language.parser.ast.AST;
import org.blade.language.parser.ast.Expr;
import org.blade.language.parser.ast.Stmt;

import java.util.*;

import static org.blade.language.parser.TokenType.*;

@SuppressWarnings("StatementWithEmptyBody")
public class Parser {
  private static final TokenType[] ASSIGNERS = new TokenType[]{
    EQUAL, PLUS_EQ, MINUS_EQ, PERCENT_EQ, DIVIDE_EQ, MULTIPLY_EQ,
    FLOOR_EQ, POW_EQ, AMP_EQ, BAR_EQ, TILDE_EQ, XOR_EQ,
    LSHIFT_EQ, RSHIFT_EQ, URSHIFT_EQ,
  };
  private static final Map<TokenType, TokenType> ASSIGNER_ALTS = new HashMap<>() {
    {
      put(PLUS_EQ, PLUS);
      put(MINUS_EQ, MINUS);
      put(PERCENT_EQ, PERCENT);
      put(DIVIDE_EQ, DIVIDE);
      put(MULTIPLY_EQ, MULTIPLY);
      put(FLOOR_EQ, FLOOR);
      put(POW_EQ, POW);
      put(AMP_EQ, AMP);
      put(BAR_EQ, BAR);
      put(TILDE_EQ, TILDE);
      put(XOR_EQ, XOR);
      put(LSHIFT_EQ, LSHIFT);
      put(RSHIFT_EQ, RSHIFT);
      put(URSHIFT_EQ, URSHIFT);
    }

    @Override
    public TokenType get(Object key) {
      return containsKey(key) ? super.get(key) : EQUAL;
    }
  };
  private static final TokenType[] OPERATORS = new TokenType[]{
    PLUS, MINUS, MULTIPLY, POW, DIVIDE, FLOOR, EQUAL, LESS,
    LSHIFT, GREATER, RSHIFT, URSHIFT, PERCENT, AMP, BAR,
    TILDE, XOR,
  };
  public final Lexer lexer;
  private final List<Token> tokens;
  private int blockCount = 0;
  private int current = 0;
  private int anonymousCount = 0;

  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.tokens = this.lexer.run();
  }

  private boolean match(TokenType... tokenTypes) {
    for (TokenType t : tokenTypes) {
      if (check(t)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private boolean check(TokenType type) {
    if (isAtEnd() && type != EOF) return false;
    return peek().type() == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type() == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw new ParserException(lexer.getSource(), peek(), false, message);
  }

  private Token consumeAny(String message, TokenType... tokenTypes) {
    for (TokenType t : tokenTypes) {
      if (check(t)) return advance();
    }
    throw new ParserException(lexer.getSource(), peek(), false, message);
  }

  private void endStatement() {
    if (
      match(EOF)
        || isAtEnd()
        || (blockCount > 0 && check(RBRACE))
    ) return;

    if (match(SEMICOLON)) {
      while (match(NEWLINE, SEMICOLON)) ;
      return;
    }

    consume(NEWLINE, "end of statement expected");

    while (match(NEWLINE, SEMICOLON)) ;
  }

  private void ignoreNewlinesNoSemi() {
    while (match(NEWLINE)) ;
  }

  private void ignoreNewlines() {
    while (match(NEWLINE) || match(SEMICOLON)) ;
  }

  private Expr.Grouping grouping() {
    return (Expr.Grouping) wrapExpr(() -> {
      ignoreNewlines();
      var expr = expression();
      ignoreNewlines();
      consume(RPAREN, "')' Expected after expression");
      return new Expr.Grouping(expr);
    });
  }

  private Expr.Call finishCall(Expr callee) {
    return (Expr.Call) wrapExpr(() -> {
      ignoreNewlines();
      List<Expr> args = new ArrayList<>();

      if (!check(RPAREN)) {
        args.add(expression());

        while (match(COMMA)) {
          ignoreNewlines();
          args.add(expression());
        }
      }

      ignoreNewlines();
      consume(RPAREN, "')' expected after args");
      return new Expr.Call(callee, args);
    });
  }

  private Expr.Index finishIndex(Expr callee) {
    return (Expr.Index) wrapExpr(() -> {
      ignoreNewlines();
      List<Expr> args = new ArrayList<>();
      args.add(expression());

      if (match(COMMA)) {
        ignoreNewlines();
        args.add(expression());
      }

      ignoreNewlines();
      consume(RBRACKET, "']' expected at end of indexer");
      return new Expr.Index(callee, args);
    });
  }

  private Expr finishDot(Expr e) {
    return (Expr) wrap((expr) -> {
      ignoreNewlines();
      var prop = new Expr.Identifier(
        consume(IDENTIFIER, "property name expected")
      );

      if (match(ASSIGNERS)) {
        Token token = previous();
        if (token.type() == EQUAL) {
          expr = new Expr.Set((Expr) expr, prop, expression());
        } else {
          expr = new Expr.Set(
            (Expr) expr,
            prop,
            new Expr.Binary(
              new Expr.Get((Expr) expr, prop),
              previous().copyToType(ASSIGNER_ALTS.get(token.type()), previous().literal()),
              assignment()
            )
          );
        }
      } else {
        expr = new Expr.Get((Expr) expr, prop);
      }

      return expr;
    }, e);
  }

  private Expr interpolation() {
    return wrapExpr(() -> {
      match(INTERPOLATION);

      Expr expr = wrapExpr(() -> new Expr.Literal(
        previous().copyToType(LITERAL, previous().literal())
      ));

      do {
        expr = new Expr.Binary(
          expr,
          previous().copyToType(PLUS, "+"),
          expression()
        );

      } while ((check(INTERPOLATION) || check(LITERAL)) && !isAtEnd());
      match(INTERPOLATION, LITERAL);

      return expr;
    });
  }

  private Expr newStatement() {
    return wrapExpr(() -> {
      Expr expr = primary();
      List<Expr> arguments = new ArrayList<>();

      consume(LPAREN, "'(' expected after new class instance");
      ignoreNewlines();

      if (!check(RPAREN)) {
        arguments.add(expression());

        while (match(COMMA)) {
          ignoreNewlines();
          arguments.add(expression());
        }
      }

      ignoreNewlines();
      consume(RPAREN, "')' expected after new class instance arguments");

      return new Expr.New(expr, arguments);
    });
  }

  private Expr literal() {
    return wrapExpr(() -> new Expr.Literal(previous()));
  }

  private Expr.Identifier identifier() { return (Expr.Identifier)wrapExpr(() -> new Expr.Identifier(previous())); }

  private Expr primary() {
    return wrapExpr(() -> {
      if (match(FALSE)) return new Expr.Boolean(false);
      if (match(TRUE)) return new Expr.Boolean(true);
      if (match(NIL)) return new Expr.Nil();
      if (match(SELF)) return new Expr.Self();
      if (match(PARENT)) return new Expr.Parent();
      if (match(NEW)) return newStatement();

      if (check(INTERPOLATION)) return interpolation();

      if (match(BIN_NUMBER, HEX_NUMBER, OCT_NUMBER, REG_NUMBER)) {
        return new Expr.Number(previous());
      }

      if (match(LITERAL)) {
        return literal();
      }

      if (match(IDENTIFIER)) return identifier();

      if (match(LPAREN)) return grouping();
      if (match(LBRACE)) return dict();
      if (match(LBRACKET)) return list();
      if (match(AT)) return anonymous();

      return null;
    });
  }

  private Expr call() {
    return wrapExpr(() -> {
      var expr = primary();

      while (true) {
        if (match(DOT)) {
          expr = finishDot(expr);
        } else if (match(LPAREN)) {
          expr = finishCall(expr);
        } else if (match(LBRACKET)) {
          expr = finishIndex(expr);
        } else {
          break;
        }
      }

      return expr;
    });
  }

  private Expr assignExpr() {
    return wrapExpr(() -> {
      Expr expr = call();

      if (match(INCREMENT)) {
        if(expr instanceof Expr.Get get) {
          expr = new Expr.Set(
            get.expression,
            get.name,
            new Expr.Binary(
              get,
              previous().copyToType(PLUS, "+"),
              new Expr.Number(previous().copyToType(REG_NUMBER, "1"))
            )
          );
        } else {
          expr = new Expr.Assign(
            expr,
            new Expr.Binary(
              expr,
              previous().copyToType(PLUS, "+"),
              new Expr.Number(previous().copyToType(REG_NUMBER, "1"))
            )
          );
        }
      } else if (match(DECREMENT)) {
        if(expr instanceof Expr.Get get) {
          expr = new Expr.Set(
            get.expression,
            get.name,
            new Expr.Binary(
              get,
              previous().copyToType(MINUS, "-"),
              new Expr.Number(previous().copyToType(REG_NUMBER, "1"))
            )
          );
        } else {
          expr = new Expr.Assign(
            expr,
            new Expr.Binary(
              expr,
              previous().copyToType(MINUS, "-"),
              new Expr.Number(previous().copyToType(REG_NUMBER, "1"))
            )
          );
        }
      }

      return expr;
    });
  }

  private Expr unary() {
    return wrapExpr(() -> {
      if (match(BANG, MINUS, TILDE)) {
        Token op = previous();
        ignoreNewlines();
        return new Expr.Unary(op, assignExpr());
      }

      return assignExpr();
    });
  }

  private Expr factor() {
    return wrapExpr(() -> {
      Expr expr = unary();

      while (match(MULTIPLY, DIVIDE, PERCENT, POW, FLOOR)) {
        Token op = previous();
        ignoreNewlines();
        expr = new Expr.Binary(expr, op, unary());
      }

      return expr;
    });
  }

  private Expr term() {
    return wrapExpr(() -> {
      Expr expr = factor();

      while (match(PLUS, MINUS)) {
        Token op = previous();
        ignoreNewlines();
        expr = new Expr.Binary(expr, op, factor());
      }

      return expr;
    });
  }

  private Expr range() {
    return wrapExpr(() -> {
      Expr expr = term();

      while (match(RANGE)) {
        ignoreNewlines();
        expr = new Expr.Range(expr, term());
      }

      return expr;
    });
  }

  private Expr shift() {
    return wrapExpr(() -> {
      Expr expr = range();

      while (match(LSHIFT, RSHIFT, URSHIFT)) {
        var op = previous();
        ignoreNewlines();
        expr = new Expr.Binary(expr, op, range());
      }

      return expr;
    });
  }

  private Expr bitAnd() {
    return wrapExpr(() -> {
      Expr expr = shift();

      while (match(AMP)) {
        var op = previous();
        ignoreNewlines();
        expr = new Expr.Binary(expr, op, shift());
      }

      return expr;
    });
  }

  private Expr bitXor() {
    return wrapExpr(() -> {
      Expr expr = bitAnd();

      while (match(XOR)) {
        var op = previous();
        ignoreNewlines();
        expr = new Expr.Binary(expr, op, bitAnd());
      }

      return expr;
    });
  }

  private Expr bitOr() {
    return wrapExpr(() -> {
      Expr expr = bitXor();

      while (match(BAR)) {
        var op = previous();
        ignoreNewlines();
        expr = new Expr.Binary(expr, op, bitXor());
      }

      return expr;
    });
  }

  private Expr comparison() {
    return wrapExpr(() -> {
      Expr expr = bitOr();

      while (match(GREATER, GREATER_EQ, LESS, LESS_EQ)) {
        Token op = previous();
        ignoreNewlines();
        expr = new Expr.Logical(expr, op, bitOr());
      }

      return expr;
    });
  }

  private Expr equality() {
    return wrapExpr(() -> {
      Expr expr = comparison();

      while (match(BANG_EQ, EQUAL_EQ)) {
        Token op = previous();
        ignoreNewlines();
        expr = new Expr.Logical(expr, op, comparison());
      }

      return expr;
    });
  }

  private Expr and() {
    return wrapExpr(() -> {
      Expr expr = equality();

      while (match(AND)) {
        Token op = previous();
        ignoreNewlines();
        expr = new Expr.Logical(expr, op, equality());
      }

      return expr;
    });
  }

  private Expr or() {
    return wrapExpr(() -> {
      Expr expr = and();

      while (match(OR)) {
        Token op = previous();
        ignoreNewlines();
        expr = new Expr.Logical(expr, op, and());
      }

      return expr;
    });
  }

  private Expr conditional() {
    return wrapExpr(() -> {
      Expr expr = or();

      if (match(QUESTION)) {
        ignoreNewlines();
        var truth = conditional();
        consume(COLON, "':' expected in ternary operation");
        ignoreNewlines();
        expr = new Expr.Condition(expr, truth, conditional());
      }

      return expr;
    });
  }

  private Expr assignment() {
    return wrapExpr(() -> {
      Expr expr = conditional();

      if (match(ASSIGNERS)) {
        var type = previous();
        ignoreNewlines();

        if (type.type() == EQUAL) {
          expr = new Expr.Assign(expr, assignment());
        } else {
          expr = new Expr.Assign(
            expr,
            new Expr.Binary(
              expr,
              previous().copyToType(ASSIGNER_ALTS.get(type.type()), previous().literal()),
              assignment()
            )
          );
        }
      }

      return expr;
    });
  }

  private Expr expression() {
    return wrapExpr(this::assignment);
  }

  private Expr dict() {
    return wrapExpr(() -> {
      ignoreNewlines();
      List<Expr> keys = new ArrayList<>();
      List<Expr> values = new ArrayList<>();

      if (!check(RBRACE)) {
        do {
          ignoreNewlines();

          if (!check(RBRACE)) {
            if (match(IDENTIFIER)) {
              keys.add(literal());
            } else {
              keys.add(expression());
            }
            ignoreNewlines();

            if (!match(COLON)) {
              values.add(literal());
            } else {
              ignoreNewlines();
              values.add(expression());
            }

            ignoreNewlines();
          }
        } while (match(COMMA));
      }

      ignoreNewlines();
      consume(RBRACE, "'}' expected after dictionary");
      return new Expr.Dict(keys, values);
    });
  }

  private Expr list() {
    return wrapExpr(() -> {
      ignoreNewlines();
      List<Expr> items = new ArrayList<>();

      if (!check(RBRACKET)) {
        do {
          ignoreNewlines();

          if (!check(RBRACKET)) {
            items.add(expression());
            ignoreNewlines();
          }
        } while (match(COMMA));
      }

      ignoreNewlines();
      consume(RBRACKET, "expected ']' at the end of list");
      return new Expr.Array(items);
    });
  }

  private Stmt echoStatement() {
    return wrapStmt(() -> {
      Expr val = expression();
      endStatement();
      return new Stmt.Echo(val);
    });
  }

  private Stmt.Expression expressionStatement(boolean is_iter) {
    return (Stmt.Expression) wrapStmt(() -> {
      Expr val = expression();
      if (!is_iter) endStatement();
      return new Stmt.Expression(val);
    });
  }

  private Stmt.Block block() {
    return (Stmt.Block) wrapStmt(() -> {
      blockCount++;

      List<Stmt> val = new ArrayList<>();
      ignoreNewlines();

      while (!check(RBRACE) && !isAtEnd()) {
        val.add(declaration());
      }

      consume(RBRACE, "'}' expected after block");
      blockCount--;

      return new Stmt.Block(val);
    });
  }

  private Stmt.Block matchBlock(String message) {
    ignoreNewlines();
    consume(LBRACE, message);
    return block();
  }

  private Stmt ifStatement() {
    return wrapStmt(() -> {
      Expr expr = expression();
      Stmt body = statement();

      if (match(ELSE)) {
        return new Stmt.If(expr, body, statement());
      }

      return new Stmt.If(expr, body, null);
    });
  }

  private Stmt whileStatement() {
    return wrapStmt(() -> new Stmt.While(expression(), statement()));
  }

  private Stmt doWhileStatement() {
    return wrapStmt(() -> {
      Stmt body = statement();
      consume(WHILE, "'while' expected after do body");
      return new Stmt.DoWhile(body, expression());
    });
  }

  private Stmt forStatement() {
    return wrapStmt(() -> {
      List<Expr.Identifier> vars = new ArrayList<>();
      vars.add(
        new Expr.Identifier(consume(IDENTIFIER, "variable name expected"))
      );

      if (match(COMMA)) {
        vars.add(
          new Expr.Identifier(consume(IDENTIFIER, "variable name expected"))
        );
      }

      consume(IN, "'in' expected after for statement variables");

      return new Stmt.For(vars, expression(), statement());
    });
  }

  private Stmt assertStatement() {
    return wrapStmt(() -> {
      Expr message = null;
      Expr expr = expression();

      if (match(COMMA)) message = expression();
      return new Stmt.Assert(expr, message);
    });
  }

  private Stmt usingStatement() {
    return wrapStmt(() -> {
      Expr expr = expression();
      List<Expr> caseLabels = new ArrayList<>();
      List<Stmt> caseBodies = new ArrayList<>();
      Stmt defaultCase = null;

      consume(LBRACE, "'{' expected after using expression");
      ignoreNewlines();

      var state = 0;

      while (!match(RBRACE) && !check(EOF)) {
        if (match(WHEN, DEFAULT, NEWLINE)) {
          if (state == 1) {
            throw new ParserException(
              lexer.getSource(),
              previous(), false, "'when' cannot exist after a default"
            );
          }

          if (previous().type() == NEWLINE) {
          } else if (previous().type() == WHEN) {
            List<Expr> tmp_cases = new ArrayList<>();
            do {
              ignoreNewlines();
              tmp_cases.add(expression());
            } while (match(COMMA));

            var stmt = statement();

            for (Expr tmp : tmp_cases) {
              caseLabels.add(tmp);
              caseBodies.add(stmt);
            }
          } else {
            state = 1;
            defaultCase = statement();
          }
        } else {
          throw new ParserException(
            lexer.getSource(),
            previous(), false, "Invalid using statement"
          );
        }
      }

      return new Stmt.Using(expr, caseLabels, caseBodies, defaultCase);
    });
  }

  private Stmt importStatement() {
    return wrapStmt(() -> {
      List<String> path = new ArrayList<>();
      List<Token> elements = new ArrayList<>();

      while (!match(NEWLINE, EOF, LBRACE)) {
        advance();
        path.add(previous().literal());
      }

      Token importsAll = null;
      int selectCount = 0;

      if (previous().type() == LBRACE) {
        var scan = true;

        while (!check(RBRACE) && scan) {
          ignoreNewlines();
          elements.add(consumeAny("identifier expected", IDENTIFIER, MULTIPLY));

          selectCount++;
          if (previous().type() == MULTIPLY) {
            if (importsAll != null) {
              throw new ParserException(
                lexer.getSource(),
                importsAll, false, "cannot repeat select all"
              );
            }

            importsAll = previous();
          }

          if (!match(COMMA)) {
            scan = false;
          }
          ignoreNewlines();
        }

        consume(RBRACE, "'}' expected at end of selective import");
      }

      if (importsAll != null && selectCount > 1) {
        throw new ParserException(
          lexer.getSource(),
          importsAll, false, "cannot import selected items and all at the same time"
        );
      }

      return new Stmt.Import(String.join("", path), elements, false);
    });
  }

  private Stmt catchStatement() {
    return wrapStmt(() -> {
      Stmt.Block body = matchBlock("'{' expected after try");
      Stmt.Block catchBody = null;
      Stmt.Block finallyBody = null;

      Expr.Identifier exception_var = null;
      if (match(CATCH)) {
        consume(IDENTIFIER, "exception variable expected");
        exception_var = identifier();

        catchBody = matchBlock("'{' expected after catch variable name");
      }

      if(exception_var == null && !check(FINALLY)) {
        throw new ParserException(lexer.getSource(), peek(), false, "try must declare at least one of `catch` or `finally`");
      }

      if (match(FINALLY)) {
        finallyBody = matchBlock("'{' expected after finally");
      }

      return new Stmt.Catch(body, catchBody, finallyBody, exception_var);
    });
  }

  private Stmt iterStatement() {
    return wrapStmt(() -> {
      if (check(LPAREN)) {
        match(LPAREN);
      }

      Stmt decl = null;
      if (!check(SEMICOLON)) {
        if (check(VAR)) {
          consume(VAR, "variable declaration expected");
        }
        decl = varDeclaration(false);
      }
      consume(SEMICOLON, "';' expected");
      ignoreNewlinesNoSemi();

      Expr condition = null;
      if (!check(SEMICOLON)) {
        condition = expression();
      }
      consume(SEMICOLON, "';' expected");
      ignoreNewlinesNoSemi();

      Stmt.Expression iterator = null;
      if (!check(LBRACE) && !check(RPAREN)) {
        do {
          iterator = expressionStatement(true);
          ignoreNewlines();
        } while (match(COMMA));
      }

      if (check(RPAREN)) {
        match(RPAREN);
      }

      Stmt.Block body = matchBlock("'{' expected at beginning of iter block");
      return new Stmt.Iter(decl, condition, iterator, body);
    });
  }

  private Stmt statement() {
    return wrapStmt(() -> {
      ignoreNewlines();

      Stmt result;

      if(match(CATCH) || match(FINALLY)) {
        throw new ParserException(
          lexer.getSource(), previous(), true,
          "`catch` and `finally` are only valid in `try` context"
        );
      }

      if (match(ECHO)) {
        result = echoStatement();
      } else if (match(IF)) {
        result = ifStatement();
      } else if (match(WHILE)) {
        result = whileStatement();
      } else if (match(DO)) {
        result = doWhileStatement();
      } else if (match(ITER)) {
        result = iterStatement();
      } else if (match(FOR)) {
        result = forStatement();
      } else if (match(USING)) {
        result = usingStatement();
      } else if (match(CONTINUE)) {
        result = new Stmt.Continue();
      } else if (match(BREAK)) {
        result = new Stmt.Break();
      } else if (match(RETURN)) {
        result = new Stmt.Return(expression());
      } else if (match(ASSERT)) {
        result = assertStatement();
      } else if (match(RAISE)) {
        result = new Stmt.Raise(expression());
      } else if (match(LBRACE)) {
        result = block();
      } else if (match(IMPORT)) {
        result = importStatement();
      } else if (match(TRY)) {
        result = catchStatement();
      } else {
        result = expressionStatement(false);
      }

      ignoreNewlines();

      return result;
    });
  }

  private Stmt varDeclaration(boolean isConstant) {
    return wrapStmt(() -> {
      consume(IDENTIFIER, "variable name expected");
      Token nameToken = previous();

      Stmt decl;
      if (match(EQUAL)) {
        decl = new Stmt.Var(nameToken, expression(), isConstant);
      } else {
        if (isConstant) {
          throw new ParserException(lexer.getSource(), peek(), false, "constant value not declared");
        }
        decl = new Stmt.Var(nameToken, null, false);
      }

      if (check(COMMA)) {
        List<Stmt> declarations = new ArrayList<>();
        declarations.add(decl);

        while (match(COMMA)) {
          ignoreNewlines();
          consume(IDENTIFIER, "variable name expected");
          nameToken = previous();

          if (match(EQUAL)) {
            declarations.add(new Stmt.Var(nameToken, expression(), isConstant));
          } else {
            if (isConstant) {
              throw new ParserException(lexer.getSource(), peek(), false, "constant value not declared");
            }
            declarations.add(new Stmt.Var(nameToken, null, false));
          }
        }

        return new Stmt.VarList(declarations);
      }

      return decl;
    });
  }

  private boolean functionArgs(List<Expr.Identifier> params) {
    ignoreNewlines();
    boolean isVariadic = false;

    while (match(IDENTIFIER, TRI_DOT)) {
      if (previous().type() == TRI_DOT) {
        consume(IDENTIFIER, "variable parameter name expected");
        isVariadic = true;
        params.add(identifier());
        break;
      }

      params.add(identifier());

      if (!check(RPAREN)) {
        consume(COMMA, "',' expected between function arguments");
        ignoreNewlines();
      }
    }

    return isVariadic;
  }

  private Expr anonymous() {
    return wrapExpr(() -> {
      Token nameCompatToken = previous();

      List<Expr.Identifier> params = new ArrayList<>();
      boolean isVariadic = false;

      if (check(LPAREN)) {
        consume(LPAREN, "expected '(' at start of anonymous function");

        if (!check(RPAREN)) {
          isVariadic = functionArgs(params);
        }

        consume(RPAREN, "expected ')' after anonymous function parameters");
      }

      var body = matchBlock("'{' expected after function declaration");

      return new Expr.Anonymous(
        new Stmt.Function(
          nameCompatToken.copyToType(IDENTIFIER, "@anon" + (anonymousCount++)),
          params, body, isVariadic
        )
      );
    });
  }

  private Stmt defDeclaration() {
    return wrapStmt(() -> {
      consume(IDENTIFIER, "function name expected");
      Token name = previous();
      List<Expr.Identifier> params = new ArrayList<>();
      boolean isVariadic = false;

      consume(LPAREN, "'(' expected after function name");
      isVariadic = functionArgs(params);
      consume(RPAREN, "')' expected after function arguments");

      var body = matchBlock("'{' expected after function declaration");

      return new Stmt.Function(name, params, body, isVariadic);
    });
  }

  private Stmt.Property classField(boolean isStatic, boolean isConst) {
    return (Stmt.Property) wrapStmt(() -> {
      consume(IDENTIFIER, "class property name expected");
      Token name = previous();

      Expr value = null;
      if (match(EQUAL)) value = expression();

      endStatement();
      ignoreNewlines();

      return new Stmt.Property(name, value, isStatic, isConst);
    });
  }

  private Stmt.Method classOperator() {
    return (Stmt.Method) wrapStmt(() -> {
      consumeAny("non-assignment operator expected", OPERATORS);
      var name = previous();

      var body = matchBlock("'{' expected after operator declaration");

      return new Stmt.Method(name, new ArrayList<>(), body, false, false);
    });
  }

  private Stmt.Method method(boolean isStatic) {
    return (Stmt.Method) wrapStmt(() -> {
      consumeAny("method name expected", IDENTIFIER, DECORATOR);
      Token name = previous();

      List<Expr.Identifier> params = new ArrayList<>();
      boolean isVariadic = false;

      consume(LPAREN, "'(' expected after method name");
      isVariadic = functionArgs(params);
      consume(RPAREN, "')' expected after method arguments");

      var body = matchBlock("'{' expected after method declaration");

      return new Stmt.Method(name, params, body, isStatic, isVariadic);
    });
  }

  private Stmt classDeclaration() {
    return wrapStmt(() -> {
      List<Stmt.Property> properties = new ArrayList<>();
      List<Stmt.Method> methods = new ArrayList<>();
      List<Stmt.Method> operators = new ArrayList<>();

      consume(IDENTIFIER, "class name expected");
      Token name = previous();
      Expr.Identifier superclass = null;

      if (match(LESS)) {
        consume(IDENTIFIER, "super class name expected");
        superclass = identifier();
      }

      ignoreNewlines();
      consume(LBRACE, "'{' expected after class declaration");
      ignoreNewlines();

      while (!check(RBRACE) && !check(EOF)) {
        boolean is_static = false;

        ignoreNewlines();

        if (match(STATIC)) is_static = true;

        if (match(VAR)) {
          properties.add(classField(is_static, false));
        } else if (match(CONST)) {
          properties.add(classField(is_static, true));
        } else if (match(DEF)) {
          operators.add(classOperator());
          ignoreNewlines();
        } else {
          methods.add(method(is_static));
          ignoreNewlines();
        }
      }

      consume(RBRACE, "'{' expected at end of class definition");
      return new Stmt.Class(name, superclass, properties, methods, operators);
    });
  }

  private Stmt declaration() {
    return wrapStmt(() -> {
      ignoreNewlines();

      Stmt result;

      if (match(VAR)) {
        result = varDeclaration(false);
      } else if (match(CONST)) {
        result = varDeclaration(true);
      } else if (match(DEF)) {
        result = defDeclaration();
      } else if (match(CLASS)) {
        result = classDeclaration();
      } else if (match(LBRACE)) {
        if (!check(NEWLINE) && blockCount == 0) {
          result = new Stmt.Expression(dict());
        } else {
          result = block();
        }
      } else {
        result = statement();
      }

      ignoreNewlines();
      return result;
    });
  }

  public List<Stmt> parse() {
    List<Stmt> result = new ArrayList<>();

    while (!isAtEnd()) {
      result.add(declaration());
    }

    return result;
  }

  @Override
  public String toString() {
    return String.format(
      "<rem::Parser path='%s' tokens=%d>",
      lexer.getSource().getPath(),
      tokens.size()
    );
  }

  private AST wrap(Callback callback, AST ast) {
    int startLine = peek().line();
    int startOffset = peek().offset();

    AST result = callback.run(ast);

    if (result != null && !result.wrapped) {
      int endLine = previous().line();
      int endOffset = previous().offset();

      result.startLine = startLine;
      result.endLine = endLine;
      result.startColumn = startOffset - lexer.source.getLineStartOffset(startLine);
      result.endColumn = endOffset - lexer.source.getLineStartOffset(endLine);

      if (result.endColumn == -1) {
        result.endLine--;
        result.endColumn = lexer.source.getLineLength(result.endLine);
      }

      if (result.startColumn == -1) {
        result.startLine--;
        result.startColumn = lexer.source.getLineLength(result.startLine);
      }

      if (result.startColumn > result.endColumn && result.startLine == result.endLine) {
        result.startColumn = result.endColumn - (result.startColumn - result.endColumn);
        if(result.startColumn < 0) {
          result.startColumn = 0;
        }
      }

      // mark ad wrapped
      result.wrapped = true;
    }

    return result;
  }

  private Expr wrapExpr(FlatCallback callback) {
    return (Expr) wrap((ignore) -> callback.run(), null);
  }

  private Stmt wrapStmt(FlatCallback callback) {
    return (Stmt) wrap((ignore) -> callback.run(), null);
  }

  interface FlatCallback {
    AST run();
  }

  interface Callback {
    AST run(AST ast);
  }
}
