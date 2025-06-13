package org.blade;

import org.graalvm.polyglot.Context;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NFunctionsTest {
  @Test
  public void calling_abs_works() {
    var context = Context.create();
    var result = context.eval("blade", "abs(-23)");

    assertEquals(23, result.asInt());
  }

  @Test
  public void a_function_can_be_called_from_Java() {
    var context = Context.create();
    var mathAbs = context.eval("blade", "abs");

    assertTrue(mathAbs.canExecute());

    var result = mathAbs.execute(-3);
    assertEquals(3, result.asInt());
  }

  @Test
  public void call_created_function_works() {
    var context = Context.create();
    var result = context.eval(
      "blade",
      "def test() { return 5 }\n" +
        "test()"
    );

    assertEquals(5L, result.asLong());
  }

  @Test
  public void can_call_created_function_with_arguments() {
    var context = Context.create();
    var result = context.eval(
      "blade",
      "def test(a, b) { return a * b }\n" +
        "test(3, 5)"
    );

    assertEquals(15L, result.asLong());
  }
}
