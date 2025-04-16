package org.nimbus;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import org.junit.jupiter.api.Test;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NRootNode;
import org.nimbus.language.nodes.expressions.arithemetic.NAddNodeGen;
import org.nimbus.language.nodes.literals.NLongLiteralNode;
import org.nimbus.language.parser.Lexer;
import org.nimbus.language.parser.Parser;
import org.nimbus.language.translator.NimTranslator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NAddNodeTest {
  @Test
  public void adds_two_numbers_correctly() {
    NNode exprNode = NAddNodeGen.create(
        new NLongLiteralNode(12),
        new NLongLiteralNode(34));
    var rootNode = new NRootNode(null, exprNode);
    CallTarget callTarget = rootNode.getCallTarget();

    var result = callTarget.call();

    assertEquals(46L, result);
  }

  @Test
  public void adding_1_to_long_max_does_not_overflow() {
    NNode exprNode = NAddNodeGen.create(
        new NLongLiteralNode(Long.MAX_VALUE),
        new NLongLiteralNode(1));
    var rootNode = new NRootNode(null, exprNode);
    CallTarget callTarget = rootNode.getCallTarget();

    var result = callTarget.call();

    assertEquals(Long.MAX_VALUE + 1D, result);
  }

  @Test
  public void add_node_should_work_from_parser() {
    try {
      var source = Source.newBuilder(NimbusLanguage.ID, "2 + 2", "<script>").build();

      var visitor = new NimTranslator(Shape.newBuilder().build());
      var parseResult = new Parser(new Lexer(source))
          .parse();
      assertEquals(1, parseResult.size());

      var callTarget = new NRootNode(null,
        parseResult.getFirst()
              .accept(visitor)
      ).getCallTarget();

      var result = callTarget.call();

      assertEquals(4L, result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
