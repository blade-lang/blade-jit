package org.nimbus;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.source.Source;
import org.junit.Test;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NBlockRootNode;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.expressions.arithemetic.NAddNodeGen;
import org.nimbus.language.nodes.literals.NIntLiteralNode;
import org.nimbus.language.nodes.statements.NBlockStmtNode;
import org.nimbus.language.parser.Lexer;
import org.nimbus.language.parser.Parser;
import org.nimbus.language.translator.NimTranslator;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class NAddNodeTest {
  @Test
  public void adds_two_numbers_correctly() {
    NNode exprNode = NAddNodeGen.create(
        new NIntLiteralNode(12),
        new NIntLiteralNode(34));
    var rootNode = new NBlockRootNode(null, new NBlockStmtNode(List.of(exprNode)), "@.script");
    CallTarget callTarget = rootNode.getCallTarget();

    var result = callTarget.call();

    assertEquals(46, result);
  }

  @Test
  public void adding_1_to_int_max_does_not_overflow() {
    NNode exprNode = NAddNodeGen.create(
        new NIntLiteralNode(Integer.MAX_VALUE),
        new NIntLiteralNode(1));
    var rootNode = new NBlockRootNode(null, new NBlockStmtNode(List.of(exprNode)), "@.script");
    CallTarget callTarget = rootNode.getCallTarget();

    var result = callTarget.call();

    assertEquals(Integer.MAX_VALUE + 1D, result);
  }

  @Test
  public void add_node_should_work_from_parser() {
    try {
      var source = Source.newBuilder(NimbusLanguage.ID, "2 + 2", "<script>").build();

      var parser = new Parser(new Lexer(source));
      var parseResult = parser.parse();
      assertEquals(1, parseResult.size());

      var visitor = new NimTranslator(parser, new NimbusLanguage().builtinObjects);
      var callTarget = new NBlockRootNode(null,
        new NBlockStmtNode(List.of(parseResult.getFirst().accept(visitor))),
        "@.script"
      ).getCallTarget();

      var result = callTarget.call();

      assertEquals(4, result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
