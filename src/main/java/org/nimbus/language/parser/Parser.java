package org.nimbus.language.parser;

import org.nimbus.language.parser.ast.Expr;
import org.nimbus.language.parser.ast.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nimbus.language.parser.TokenType.*;

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
  private final Lexer lexer;
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

    while (match(NEWLINE, SEMICOLON));
  }

  private void ignoreNewlinesNoSemi() {
    while (match(NEWLINE)) ;
  }

  private void ignoreNewlines() {
    while (match(NEWLINE) || match(SEMICOLON)) ;
  }

  private Expr.Grouping grouping() {
    ignoreNewlines();
    var expr = expression();
    ignoreNewlines();
    consume(RPAREN, "')' Expected after expression");
    return new Expr.Grouping(expr);
  }

  private Expr.Call finishCall(Expr callee) {
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
  }

  private Expr.Index finishIndex(Expr callee) {
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
  }

  private Expr finishDot(Expr expr) {
    ignoreNewlines();
    var prop = new Expr.Identifier(
      consume(IDENTIFIER, "property name expected")
    );

    if (match(ASSIGNERS)) {
      expr = new Expr.Set(expr, prop, expression());
    } else {
      expr = new Expr.Get(expr, prop);
    }

    return expr;
  }

  private Expr interpolation() {
    match(INTERPOLATION);

    Expr expr = new Expr.Literal(
      previous().copyToType(LITERAL, previous().literal().substring(1, previous().literal().length() - 1))
    );

    do {
      expr = new Expr.Binary(
        expr,
        previous().copyToType(PLUS, "+"),
        expression()
      );

    } while ((check(INTERPOLATION) || check(LITERAL)) && !isAtEnd());
    match(INTERPOLATION, LITERAL);

    return expr;
  }

  private Expr newStatement() {
    Expr expr = primary();
    List<Expr> arguments = new ArrayList<>();

    consume(LPAREN, "'(' expected after new class instance");
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
  }

  private Expr primary() {
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
      return new Expr.Literal(previous());
    }

    if (match(IDENTIFIER)) return new Expr.Identifier(previous());

    if (match(LPAREN)) return grouping();
    if (match(LBRACE)) return dict();
    if (match(LBRACKET)) return list();
    if (match(AT)) return anonymous();

    return null;
  }

  private Expr call() {
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
  }

  private Expr assignExpr() {
    Expr expr = call();

    if (match(INCREMENT)) {
      expr = new Expr.Assign(
        expr,
        new Expr.Binary(
          expr,
          previous().copyToType(PLUS, "+"),
          new Expr.Number(previous().copyToType(REG_NUMBER, "1"))
        )
      );
    } else if (match(DECREMENT)) {
      expr = new Expr.Assign(
        expr,
        new Expr.Binary(
          expr,
          previous().copyToType(MINUS, "-"),
          new Expr.Number(previous().copyToType(REG_NUMBER, "1"))
        )
      );
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS, TILDE)) {
      Token op = previous();
      ignoreNewlines();
      return new Expr.Unary(op, assignExpr());
    }

    return assignExpr();
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(MULTIPLY, DIVIDE, PERCENT, POW, FLOOR)) {
      Token op = previous();
      ignoreNewlines();
      expr = new Expr.Binary(expr, op, unary());
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(PLUS, MINUS)) {
      Token op = previous();
      ignoreNewlines();
      expr = new Expr.Binary(expr, op, factor());
    }

    return expr;
  }

  private Expr range() {
    Expr expr = term();

    while (match(RANGE)) {
      ignoreNewlines();
      expr = new Expr.Range(expr, term());
    }

    return expr;
  }

  private Expr shift() {
    Expr expr = range();

    while (match(LSHIFT, RSHIFT, URSHIFT)) {
      var op = previous();
      ignoreNewlines();
      expr = new Expr.Binary(expr, op, range());
    }

    return expr;
  }

  private Expr bitAnd() {
    Expr expr = shift();

    while (match(AMP)) {
      var op = previous();
      ignoreNewlines();
      expr = new Expr.Binary(expr, op, shift());
    }

    return expr;
  }

  private Expr bitXor() {
    Expr expr = bitAnd();

    while (match(XOR)) {
      var op = previous();
      ignoreNewlines();
      expr = new Expr.Binary(expr, op, bitAnd());
    }

    return expr;
  }

  private Expr bitOr() {
    Expr expr = bitXor();

    while (match(BAR)) {
      var op = previous();
      ignoreNewlines();
      expr = new Expr.Binary(expr, op, bitXor());
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = bitOr();

    while (match(GREATER, GREATER_EQ, LESS, LESS_EQ)) {
      Token op = previous();
      ignoreNewlines();
      expr = new Expr.Logical(expr, op, bitOr());
    }

    return expr;
  }

  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQ, EQUAL_EQ)) {
      Token op = previous();
      ignoreNewlines();
      expr = new Expr.Logical(expr, op, comparison());
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token op = previous();
      ignoreNewlines();
      expr = new Expr.Logical(expr, op, equality());
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token op = previous();
      ignoreNewlines();
      expr = new Expr.Logical(expr, op, and());
    }

    return expr;
  }

  private Expr conditional() {
    Expr expr = or();

    if (match(QUESTION)) {
      ignoreNewlines();
      var truth = conditional();
      consume(COLON, "':' expected in ternary operation");
      ignoreNewlines();
      expr = new Expr.Condition(expr, truth, conditional());
    }

    return expr;
  }

  private Expr assignment() {
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
  }

  private Expr expression() {
    return assignment();
  }

  private Expr dict() {
    ignoreNewlines();
    List<Expr> keys = new ArrayList<>();
    List<Expr> values = new ArrayList<>();

    if (!check(RBRACE)) {
      do {
        ignoreNewlines();

        if (!check(RBRACE)) {
          if (match(IDENTIFIER)) {
            keys.add(new Expr.Literal(previous()));
          } else {
            keys.add(expression());
          }
          ignoreNewlines();

          if (!match(COLON)) {
            values.add(new Expr.Literal(previous()));
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
  }

  private Expr list() {
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
  }

  private Stmt echoStatement() {
    Expr val = expression();
    endStatement();
    return new Stmt.Echo(val);
  }

  private Stmt.Expression expressionStatement(boolean is_iter) {
    Expr val = expression();
    if (!is_iter) endStatement();
    return new Stmt.Expression(val);
  }

  private Stmt.Block block() {
    blockCount++;

    List<Stmt> val = new ArrayList<>();
    ignoreNewlines();

    while (!check(RBRACE) && !isAtEnd()) {
      val.add(declaration());
    }

    consume(RBRACE, "'}' expected after block");
    blockCount--;

    return new Stmt.Block(val);
  }

  private Stmt ifStatement() {
    Expr expr = expression();
    Stmt body = statement();

    if (match(ELSE)) {
      return new Stmt.If(expr, body, statement());
    }

    return new Stmt.If(expr, body, null);
  }

  private Stmt whileStatement() {
    return new Stmt.While(expression(), statement());
  }

  private Stmt doWhileStatement() {
    Stmt body = statement();
    consume(WHILE, "'while' expected after do body");
    return new Stmt.DoWhile(body, expression());
  }

  private Stmt forStatement() {
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
  }

  private Stmt assertStatement() {
    Expr message = null;
    Expr expr = expression();

    if (match(COMMA)) message = expression();
    return new Stmt.Assert(expr, message);
  }

  private Stmt usingStatement() {
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
  }

  private Stmt importStatement() {
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
  }

  private Stmt catchStatement() {
    consume(LBRACE, "'{' expected after catch");
    Stmt.Block body = block();

    Expr.Identifier exception_var = null;
    if (match(AS)) {
      if (check(IDENTIFIER)) {
        consume(IDENTIFIER, "exception variable expected");
        exception_var = new Expr.Identifier(previous());
      }
    }

    return new Stmt.Catch(body, exception_var);
  }

  private Stmt iterStatement() {
    if(check(LPAREN)) {
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

    if(check(RPAREN)) {
      match(RPAREN);
    }

    consume(LBRACE, "'{' expected after catch");
    Stmt.Block body = block();
    return new Stmt.Iter(decl, condition, iterator, body);
  }

  private Stmt statement() {
    ignoreNewlines();

    Stmt result;

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
    } else if (match(CATCH)) {
      result = catchStatement();
    } else {
      result = expressionStatement(false);
    }

    ignoreNewlines();

    return result;
  }

  private Stmt varDeclaration(boolean isConstant) {
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
  }

  private Expr anonymous() {
    Token nameCompatToken = previous();

    List<Expr.Identifier> params = new ArrayList<>();
    boolean isVariadic = false;

    if (check(LPAREN)) {
      consume(LPAREN, "expected '(' at start of anonymous function");

      while (!check(RPAREN)) {
        consumeAny("parameter name expected", IDENTIFIER, TRI_DOT);
        if (previous().type() == TRI_DOT) {
          isVariadic = true;
          break;
        }

        params.add(new Expr.Identifier(previous()));

        if (!check(RPAREN)) {
          consume(COMMA, "',' expected between function params");
        }
      }

      consume(RPAREN, "expected ')' after anonymous function parameters");
    }

    consume(LBRACE, "'{' expected after function declaration");
    var body = block();

    return new Expr.Anonymous(
      new Stmt.Function(
        nameCompatToken.copyToType(IDENTIFIER, "@anon" + (anonymousCount++)),
        params, body, isVariadic
      )
    );
  }

  private Stmt defDeclaration() {
    consume(IDENTIFIER, "function name expected");
    Token name = previous();
    List<Expr.Identifier> params = new ArrayList<>();
    boolean isVariadic = false;

    consume(LPAREN, "'(' expected after function name");
    while (match(IDENTIFIER, TRI_DOT)) {
      if (previous().type() == TRI_DOT) {
        isVariadic = true;
        break;
      }

      params.add(new Expr.Identifier(previous()));

      if (!check(RPAREN)) {
        consume(COMMA, "',' expected between function arguments");
        ignoreNewlines();
      }
    }

    consume(RPAREN, "')' expected after function arguments");
    consume(LBRACE, "'{' expected after function declaration");
    var body = block();

    return new Stmt.Function(name, params, body, isVariadic);
  }

  private Stmt.Property classField(boolean isStatic, boolean isConst) {
    consume(IDENTIFIER, "class property name expected");
    Token name = previous();

    Expr value = null;
    if (match(EQUAL)) value = expression();

    endStatement();
    ignoreNewlines();

    return new Stmt.Property(name, value, isStatic, isConst);
  }

  private Stmt.Method classOperator() {
    consumeAny("non-assignment operator expected", OPERATORS);
    var name = previous();

    consume(LBRACE, "'{' expected after operator declaration");
    var body = block();

    return new Stmt.Method(name, new ArrayList<>(), body, false, false);
  }

  private Stmt.Method method(boolean isStatic) {
    consumeAny("method name expected", IDENTIFIER, DECORATOR);
    Token name = previous();

    List<Expr.Identifier> params = new ArrayList<>();
    boolean isVariadic = false;

    consume(LPAREN, "'(' expected after method name");
    while (match(IDENTIFIER, TRI_DOT)) {
      if (previous().type() == TRI_DOT) {
        isVariadic = true;
        break;
      }

      params.add(new Expr.Identifier(previous()));

      if (!check(RPAREN)) {
        consume(COMMA, "',' expected between method arguments");
        ignoreNewlines();
      }
    }

    consume(RPAREN, "')' expected after method arguments");
    consume(LBRACE, "'{' expected after method declaration");
    var body = block();

    return new Stmt.Method(name, params, body, isStatic, isVariadic);
  }

  private Stmt classDeclaration() {
    List<Stmt.Property> properties = new ArrayList<>();
    List<Stmt.Method> methods = new ArrayList<>();
    List<Stmt.Method> operators = new ArrayList<>();

    consume(IDENTIFIER, "class name expected");
    Token name = previous();
    Expr.Identifier superclass = null;

    if (match(LESS)) {
      consume(IDENTIFIER, "super class name expected");
      superclass = new Expr.Identifier(previous());
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
  }

  private Stmt declaration() {
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
  }

  public List<Stmt> parse() {
    List<Stmt> result = new ArrayList<>();

    while (!isAtEnd()) {
      Stmt declaration = declaration();
      result.add(declaration);
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
}
