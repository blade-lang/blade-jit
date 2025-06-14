package org.blade;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.source.Source;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NBlockRootNode;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.expressions.arithemetic.NAddNodeGen;
import org.blade.language.nodes.literals.NLongLiteralNode;
import org.blade.language.nodes.statements.NBlockStmtNode;
import org.blade.language.parser.Lexer;
import org.blade.language.parser.Parser;
import org.blade.language.translator.BladeTranslator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class NAddNodeTest {
  @Test
  public void adds_two_numbers_correctly() {
    NNode exprNode = NAddNodeGen.create(
      new NLongLiteralNode(12),
      new NLongLiteralNode(34)
    );
    var rootNode = new NBlockRootNode(null, new NBlockStmtNode(List.of(exprNode)), "@.script");
    CallTarget callTarget = rootNode.getCallTarget();

    var result = callTarget.call();

    assertEquals(46L, result);
  }

  @Test
  public void adding_1_to_long_max_does_not_overflow() {
    NNode exprNode = NAddNodeGen.create(
      new NLongLiteralNode(Long.MAX_VALUE),
      new NLongLiteralNode(1)
    );
    var rootNode = new NBlockRootNode(null, new NBlockStmtNode(List.of(exprNode)), "@.script");
    CallTarget callTarget = rootNode.getCallTarget();

    var result = callTarget.call();

    assertEquals(Long.MAX_VALUE + 1D, result);
  }

  @Test
  public void add_node_should_work_from_parser() {
    try {
      var source = Source.newBuilder(BladeLanguage.ID, "2 + 2", "<script>").build();

      var parser = new Parser(new Lexer(source), new BladeLanguage());
      var parseResult = parser.parse();
      assertEquals(1, parseResult.size());

      var visitor = new BladeTranslator(parser, new BladeLanguage().builtinObjects);
      var callTarget = new NBlockRootNode(
        null,
        new NBlockStmtNode(List.of(parseResult.get(0).accept(visitor))),
        "@.script"
      ).getCallTarget();

      var result = callTarget.call();

      assertEquals(4L, result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
