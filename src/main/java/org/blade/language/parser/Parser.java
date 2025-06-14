package org.blade.language.parser;

import org.blade.language.BladeLanguage;
import org.blade.language.parser.ast.AST;
import org.blade.language.parser.ast.Expr;
import org.blade.language.parser.ast.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    PLUS, MINUS, MULTIPLY, POW, DIVIDE, FLOOR, EQUAL_EQ, LESS,
    LSHIFT, GREATER, RSHIFT, URSHIFT, PERCENT, AMP, BAR,
    TILDE, XOR,
  };
  public final Lexer lexer;
  private final List<Token> tokens;
  private final BladeLanguage language;
  private int blockCount = 0;
  private int current = 0;
  private int anonymousCount = 0;

  public Parser(Lexer lexer, BladeLanguage language) {
    this.language = language;
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
    return wrap(() -> {
      ignoreNewlines();
      var expr = expression();
      ignoreNewlines();
      consume(RPAREN, "')' Expected after expression");
      return new Expr.Grouping(expr);
    });
  }

  private Expr.Call finishCall(Expr callee) {
    return wrap(() -> {
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

  private Expr finishIndex(Expr callee) {
    return wrap(() -> {
      ignoreNewlines();
      Expr expr = expression();

      if (match(COMMA)) {
        ignoreNewlines();
        expr = new Expr.Slice(callee, expr, expression());
      } else {
        expr = new Expr.Index(callee, expr);
      }

      ignoreNewlines();
      consume(RBRACKET, "']' expected at end of indexer");
      return expr;
    });
  }

  private Expr finishDot(Expr e) {
    return wrap(
      (expr) -> {
        ignoreNewlines();
        var prop = new Expr.Identifier(
          consume(IDENTIFIER, "property name expected")
        );

        if (match(ASSIGNERS)) {
          Token token = previous();
          if (token.type() == EQUAL) {
            expr = new Expr.Set(expr, prop, expression());
          } else {
            expr = new Expr.Set(
              expr,
              prop,
              new Expr.Binary(
                new Expr.Get(expr, prop),
                previous().copyToType(ASSIGNER_ALTS.get(token.type()), previous().literal()),
                assignment()
              )
            );
          }
        } else {
          expr = new Expr.Get(expr, prop);
        }

        return expr;
      }, e
    );
  }

  private Expr interpolation() {
    return wrap(() -> {
      match(INTERPOLATION);

      Expr expr = wrap(() -> new Expr.Literal(
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
    return wrap(() -> {
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
    return wrap(() -> new Expr.Literal(previous()));
  }

  private Expr.Identifier identifier() {
    return wrap(() -> new Expr.Identifier(previous()));
  }

  private Expr primary() {
    return wrap(() -> {
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

      if (match(BIG_NUMBER)) {
        return new Expr.BigNumber(previous());
      }

      if (match(LITERAL)) {
        return literal();
      }

      if (match(IDENTIFIER)) return identifier();

      if (match(LPAREN)) return grouping();
      if (match(LBRACE)) {
        return dict();
      }
      if (match(LBRACKET)) return list();
      if (match(AT)) return anonymous();

      return null;
    });
  }

  private Expr range() {
    return wrap(() -> {
      Expr expr = primary();

      while (match(RANGE)) {
        ignoreNewlines();
        expr = new Expr.Range(expr, primary());
      }

      return expr;
    });
  }

  private Expr doCall(Expr e) {
    return wrap(
      (expr) -> {
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
      }, e
    );
  }

  private Expr call() {
    return wrap(() -> doCall(range()));
  }

  private Expr assignExpr() {
    return wrap(() -> {
      Expr expr = call();

      if (match(INCREMENT)) {
        if (expr instanceof Expr.Get get) {
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
        if (expr instanceof Expr.Get get) {
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
    return wrap(() -> {
      if (match(BANG, MINUS, TILDE)) {
        Token op = previous();
        ignoreNewlines();
        return new Expr.Unary(op, unary());
      }

      return assignExpr();
    });
  }

  private Expr factor() {
    return wrap(() -> {
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
    return wrap(() -> {
      Expr expr = factor();

      while (match(PLUS, MINUS)) {
        Token op = previous();
        ignoreNewlines();
        expr = new Expr.Binary(expr, op, factor());
      }

      return expr;
    });
  }

  private Expr shift() {
    return wrap(() -> {
      Expr expr = term();

      while (match(LSHIFT, RSHIFT, URSHIFT)) {
        var op = previous();
        ignoreNewlines();
        expr = new Expr.Binary(expr, op, term());
      }

      return expr;
    });
  }

  private Expr bitAnd() {
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(this::assignment);
  }

  private Expr dict() {
    return wrap(() -> {
      ignoreNewlines();
      List<Expr> keys = new ArrayList<>();
      List<Expr> values = new ArrayList<>();

      if (!check(RBRACE)) {
        do {
          ignoreNewlines();

          if (!check(RBRACE)) {
            Expr key;
            if (match(IDENTIFIER)) {
              key = literal();
            } else {
              key = expression();
            }
            keys.add(key);
            ignoreNewlines();

            if (!match(COLON)) {
              if (key instanceof Expr.Literal literal) {
                values.add(new Expr.Identifier(literal.token));
              } else {
                throw new ParserException(
                  lexer.getSource(),
                  previous(), false, "missing value in dictionary definition"
                );
              }
            } else {
              ignoreNewlines();
              values.add(expression());
            }

            ignoreNewlines();
          } else {
            break;
          }
        } while (match(COMMA));
      }

      if (keys.size() != values.size()) {
        throw new ParserException(
          lexer.getSource(),
          previous(), false, "key/value count mismatch dictionary definition"
        );
      }

      ignoreNewlines();
      consume(RBRACE, "'}' expected after dictionary");
      return new Expr.Dict(keys, values);
    });
  }

  private Expr list() {
    return wrap(() -> {
      ignoreNewlines();
      List<Expr> items = new ArrayList<>();

      if (!check(RBRACKET)) {
        do {
          ignoreNewlines();

          if (!check(RBRACKET)) {
            items.add(expression());
            ignoreNewlines();
          } else {
            break;
          }
        } while (match(COMMA));
      }

      ignoreNewlines();
      consume(RBRACKET, "expected ']' at the end of list");
      return new Expr.Array(items);
    });
  }

  private Stmt echoStatement() {
    return wrap(() -> {
      Expr val = expression();
      endStatement();
      return new Stmt.Echo(val);
    });
  }

  private Stmt.Expression expressionStatement(boolean is_iter) {
    return wrap(() -> {
      Expr val = expression();
      if (!is_iter) endStatement();
      return new Stmt.Expression(val);
    });
  }

  private Stmt.Block block() {
    return wrap(() -> {
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
    return wrap(() -> {
      Expr expr = expression();
      Stmt body = statement();

      if (match(ELSE)) {
        return new Stmt.If(expr, body, statement());
      }

      return new Stmt.If(expr, body, null);
    });
  }

  private Stmt whileStatement() {
    return wrap(() -> new Stmt.While(expression(), statement()));
  }

  private Stmt doWhileStatement() {
    return wrap(() -> {
      Stmt body = statement();
      consume(WHILE, "'while' expected after do body");
      return new Stmt.DoWhile(body, expression());
    });
  }

  private Stmt forStatement() {
    return wrap(() -> {
      consume(IDENTIFIER, "variable name expected");

      // var key = nil
      Stmt.Var key = new Stmt.Var(previous().copyToType(IDENTIFIER, " key "), null, false);
      // var value = nil
      Stmt.Var value = new Stmt.Var(previous(), null, false);

      if (match(COMMA)) {
        consume(IDENTIFIER, "variable name expected");
        key = value;
        value = new Stmt.Var(previous(), null, false);
      }

      consume(IN, "'in' expected after for statement variables");

      // object
      Expr iterable = expression();

      List<Stmt> stmtList = new ArrayList<>();

      // key = object.@key(key)
      stmtList.add(new Stmt.Expression(
        new Expr.Assign(
          new Expr.Identifier(key.name),
          new Expr.Call(
            new Expr.Get(
              iterable,
              new Expr.Identifier(previous().copyToType(IDENTIFIER, "@key"))
            ),
            List.of(new Expr.Identifier(key.name))
          )
        )
      ));

      // if key == nil {
      //   break
      // }
      stmtList.add(new Stmt.If(
        new Expr.Binary(
          new Expr.Identifier(key.name),
          key.name.copyToType(EQUAL_EQ, "=="),
          new Expr.Nil()
        ),
        new Stmt.Break(),
        null
      ));

      // value = object.@value(key)
      stmtList.add(new Stmt.Expression(
        new Expr.Assign(
          new Expr.Identifier(value.name),
          new Expr.Call(
            new Expr.Get(
              iterable,
              new Expr.Identifier(previous().copyToType(IDENTIFIER, "@value"))
            ),
            List.of(new Expr.Identifier(key.name))
          )
        )
      ));

      // parse the loop body
      stmtList.add(statement());

      return new Stmt.Block(
        List.of(
          key,
          value,
          new Stmt.While(new Expr.Boolean(true), new Stmt.Block(stmtList))
        )
      );
    });
  }

  private Stmt assertStatement() {
    return wrap(() -> {
      Expr message = null;
      Expr expr = expression();

      if (match(COMMA)) message = expression();
      return new Stmt.Assert(expr, message);
    });
  }

  private Stmt usingStatement() {
    return wrap(() -> {
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
    return wrap(() -> {
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
    return wrap(() -> {
      Stmt.Block body = matchBlock("'{' expected after try");
      Stmt.Block catchBody = null;
      Stmt.Block finallyBody = null;

      Expr.Identifier exception_var = null;
      if (match(CATCH)) {
        consume(IDENTIFIER, "exception variable expected");
        exception_var = identifier();

        catchBody = matchBlock("'{' expected after catch variable name");
      }

      if (exception_var == null && !check(FINALLY)) {
        throw new ParserException(
          lexer.getSource(),
          peek(),
          false,
          "try must declare at least one of `catch` or `finally`"
        );
      }

      if (match(FINALLY)) {
        finallyBody = matchBlock("'{' expected after finally");
      }

      return new Stmt.Catch(body, catchBody, finallyBody, exception_var);
    });
  }

  private Stmt iterStatement() {
    return wrap(() -> {
      if (check(LPAREN)) {
        match(LPAREN);
      }

      Stmt declaration = null;
      if (!check(SEMICOLON)) {
        if (check(VAR)) {
          consume(VAR, "variable declaration expected");
        }
        declaration = varDeclaration(false);
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
      return new Stmt.Iter(declaration, condition, iterator, body);
    });
  }

  private Stmt statement() {
    return wrap(() -> {
      ignoreNewlines();

      Stmt result;

      if (match(CATCH) || match(FINALLY)) {
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
    return wrap(() -> {
      consume(IDENTIFIER, "variable name expected");
      Token nameToken = previous();

      Stmt declaration;
      if (match(EQUAL)) {
        declaration = new Stmt.Var(nameToken, expression(), isConstant);
      } else {
        if (isConstant) {
          throw new ParserException(lexer.getSource(), peek(), false, "constant value not declared");
        }
        declaration = new Stmt.Var(nameToken, null, false);
      }

      if (check(COMMA)) {
        List<Stmt> declarations = new ArrayList<>();
        declarations.add(declaration);

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

      return declaration;
    });
  }

  private boolean functionArgs(List<Expr.Identifier> params, List<Expr.Identifier> types) {
    ignoreNewlines();
    boolean isVariadic = false;

    while (match(IDENTIFIER, TRI_DOT)) {
      if (previous().type() == TRI_DOT) {
        consume(IDENTIFIER, "variable parameter name expected");
        isVariadic = true;
        params.add(identifier());
        types.add(null);
        break;
      }

      params.add(identifier());

      if (match(COLON)) {
        consume(IDENTIFIER, "function argument type name expected");
        types.add(identifier());
      } else {
        types.add(null);
      }

      if (!check(RPAREN)) {
        consume(COMMA, "',' expected between function arguments");
        ignoreNewlines();
      }
    }

    return isVariadic;
  }

  private void processFunctionParamTypes(Stmt.Block block, List<Expr.Identifier> params, List<Expr.Identifier> types) {
    if (language.enforceTypes) {
      for (int i = types.size() - 1; i >= 0; i--) {
        Expr.Identifier current = types.get(i);
        if (current != null) {
          block.body.addFirst(
            new Stmt.If(
              new Expr.Logical(params.get(i), current.token.copyToType(BANG_EQ, "!="), new Expr.Nil()),
              new Stmt.Assert(
                new Expr.Logical(
                  new Expr.Call(
                    new Expr.Get(
                      params.get(i),
                      new Expr.Identifier(current.token.copyToType(IDENTIFIER, "get_class"))
                    ),
                    List.of()
                  ),
                  current.token.copyToType(EQUAL_EQ, "=="),
                  current
                ),
                new Expr.New(
                  new Expr.Identifier(current.token.copyToType(IDENTIFIER, "TypeError")),
                  List.of(new Expr.Literal(current.token.copyToType(
                    LITERAL,
                    "Expected type of " + current.token.literal() + " as argument " + (i + 1) + " (" + params.get(i).token.literal() + ")"
                  )))
                )
              ),
              null
            )
          );
        }
      }
    }
  }

  private Expr anonymous() {
    return wrap(() -> {
      Token nameCompatToken = previous();

      List<Expr.Identifier> params = new ArrayList<>();
      List<Expr.Identifier> paramTypes = new ArrayList<>();
      boolean isVariadic = false;

      if (check(LPAREN)) {
        consume(LPAREN, "expected '(' at start of anonymous function");

        if (!check(RPAREN)) {
          isVariadic = functionArgs(params, paramTypes);
        }

        consume(RPAREN, "expected ')' after anonymous function parameters");
      }

      var body = matchBlock("'{' expected after function declaration");
      processFunctionParamTypes(body, params, paramTypes);

      return new Expr.Anonymous(
        new Stmt.Function(
          nameCompatToken.copyToType(IDENTIFIER, "@anon" + (anonymousCount++)),
          params, body, isVariadic
        )
      );
    });
  }

  private Stmt defDeclaration() {
    return wrap(() -> {
      consume(IDENTIFIER, "function name expected");
      Token name = previous();
      List<Expr.Identifier> params = new ArrayList<>();
      List<Expr.Identifier> paramTypes = new ArrayList<>();

      consume(LPAREN, "'(' expected after function name");
      boolean isVariadic = functionArgs(params, paramTypes);
      consume(RPAREN, "')' expected after function arguments");

      var body = matchBlock("'{' expected after function declaration");
      processFunctionParamTypes(body, params, paramTypes);

      return new Stmt.Function(name, params, body, isVariadic);
    });
  }

  private Stmt.Property classField(boolean isStatic, boolean isConst) {
    return wrap(() -> {
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
    return wrap(() -> {
      consumeAny("non-assignment operator expected", OPERATORS);
      var name = previous();

      List<Expr.Identifier> params = new ArrayList<>();
      List<Expr.Identifier> paramTypes = new ArrayList<>();
      params.add(new Expr.Identifier(previous().copyToType(IDENTIFIER, "__arg__")));
      paramTypes.add(null);

      var body = matchBlock("'{' expected after operator declaration");
      processFunctionParamTypes(body, params, paramTypes);

      return new Stmt.Method(name, params, body, false, false);
    });
  }

  private Stmt.Method method(boolean isStatic) {
    return wrap(() -> {
      consumeAny("method name expected", IDENTIFIER, DECORATOR);
      Token name = previous();

      List<Expr.Identifier> params = new ArrayList<>();
      List<Expr.Identifier> paramTypes = new ArrayList<>();

      consume(LPAREN, "'(' expected after method name");
      boolean isVariadic = functionArgs(params, paramTypes);
      consume(RPAREN, "')' expected after method arguments");

      var body = matchBlock("'{' expected after method declaration");
      processFunctionParamTypes(body, params, paramTypes);

      return new Stmt.Method(name, params, body, isStatic, isVariadic);
    });
  }

  private Stmt classDeclaration() {
    return wrap(() -> {
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
    return wrap(() -> {
      ignoreNewlines();

      Stmt result;

      if (match(VAR)) {
        result = varDeclaration(false);
        endStatement();
      } else if (match(CONST)) {
        result = varDeclaration(true);
        endStatement();
      } else if (match(DEF)) {
        result = defDeclaration();
      } else if (match(CLASS)) {
        result = classDeclaration();
      } else if (match(LBRACE)) {
        if (!check(NEWLINE) && blockCount == 0) {
          result = new Stmt.Expression(doCall(dict()));
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

  private <T extends AST> T wrap(Callback<T> callback, T ast) {
    int startLine = peek().line();
    int startOffset = peek().offset();

    T result = callback.run(ast);

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
        if (result.startColumn < 0) {
          result.startColumn = 0;
        }
      }

      // mark ad wrapped
      result.wrapped = true;
    }

    return result;
  }

  private <T extends AST> T wrap(FlatCallback<T> callback) {
    return wrap((ignore) -> callback.run(), null);
  }

  interface FlatCallback<T> {
    T run();
  }

  interface Callback<T> {
    T run(T ast);
  }
}
