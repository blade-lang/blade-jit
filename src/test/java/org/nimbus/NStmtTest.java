package org.nimbus;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NStmtTest {
  @Test
  public void evaluate_statements() {
    var context = Context.create();
    var result = context.eval("nim",
      """
        var a = 0
        var b
        const c = 2.0
        b = 1
        a + b + c
        """
    );

    assertEquals(3.0, result.asDouble(), 0.0);
  }
}
