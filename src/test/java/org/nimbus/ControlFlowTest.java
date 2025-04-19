package org.nimbus;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ControlFlowTest {
  private Context context;

  @BeforeEach
  public void setup() {
    context = Context.create();
  }

  @AfterEach
  public void tearDown() {
    context.close();
  }

  @Test
  public void var_declarations_are_local_in_nested_blocks() {
    var result = context.eval("nim",
      """
        var v = 3
        {
          var v = 5
        }
        v"""
    );
    assertEquals(3L, result.asLong());
  }

  @Test
  public void var_declarations_are_local_in_nested_blocks_of_functions() {
    var result = context.eval("nim",
      """
        def f() {
          var v = 3
          {
            var v = 5
          }
          return v
        }
        
        f()"""
    );
    assertEquals(3L, result.asLong());
  }

  @Test
  public void a_function_is_equal_to_itself_but_not_lte() {
    context.eval("nim",
      """
        def f() {
          return false
        }
        var t1 = f == f
        var f1 = f < f
        var f2 = f <= f
       """
    );

    var bindings = context.getBindings("nim");
    assertTrue(bindings.getMember("t1").asBoolean());
    assertFalse(bindings.getMember("f1").asBoolean());
    assertFalse(bindings.getMember("f2").asBoolean());
  }

  @Test
  public void if_in_a_function_works() {
    this.context.eval("nim",
      """
        def sig(n) {
            if n < 0 return -1
            else if n > 0 return 1
            else return 0
        }
        var s1 = sig(34)
        var s2 = sig(0)
        var s3 = sig(-12)
        """
    );

    var bindings = context.getBindings("nim");
    assertEquals(1, bindings.getMember("s1").asInt());
    assertEquals(0, bindings.getMember("s2").asInt());
    assertEquals(-1, bindings.getMember("s3").asInt());
  }

  @Test
  public void iterative_fibonacci_works() {
    var result = context.eval("nim",
      """
        def fib(n) {
            if n < 2 {
                return n
            }
            var a = 0, b = 1, i = 2
            while i <= n {
                var f = a + b
                a = b
                b = f
                i = i + 1
            }
            return b
        }
        fib(7)"""
    );

    assertEquals(13, result.asInt());
  }

  @Test
  public void do_while_always_executes_at_least_once() {
    var result = context.eval("nim",
      """
        def f(n) {
            var ret = n + 2
            do {
                ret = n + 4
            } while false
            return ret
        }
        f(8)"""
    );
    
    assertEquals(12, result.asInt());
  }

  @Test
  public void iter_parts_are_all_optional() {
    var result = context.eval("nim",
      """
        def fib(n) {
            if n < 2 {
                return n
            }
            var a = 0, b = 1, i = 2
            iter ;; {
                var f = a + b
                a = b
                b = f
                i = i + 1
                if i > n
                    break
                else
                    continue
            }
            return b
        }
        fib(8)"""
    );

    assertEquals(21, result.asInt());
  }

  @Test
  public void for_loop_executes_as_expected() {
    var result = context.eval("nim",
      """
        def fib(n) {
            if n < 2 {
                return n
            }
            var a = 0, b = 1
            iter var i = 2; i <= n; i = i + 1 {
                const f = a + b
                a = b
                b = f
            }
            return b
        }
        fib(6)"""
    );

    assertEquals(8, result.asInt());
  }

  @Test
  public void recursive_fibonacci_works() {
    var result = context.eval("nim",
      """
        def fib(n) {
            if n > -2 {
                return abs(n)
            }
            return fib(n + 1) + fib(n + 2)
        }
        fib(-9)"""
    );

    assertEquals(34, result.asInt());
  }

  @Test
  public void if_statement_returns_value() {
    var result = context.eval("nim",
      """
        if true {
            42
        }"""
    );

    assertTrue(result.fitsInInt());
    assertEquals(42, result.asInt());
  }

  @Test
  public void return_statement_is_not_allowed_on_top_level() {
    try {
      context.eval("nim",
        "return"
      );

      fail("expected PolyglotException to be thrown");
    } catch (PolyglotException e) {
      assertTrue(e.isGuestException());
      assertFalse(e.isInternalError());
      assertEquals("return is not allowed in this scope", e.getMessage());
    }
  }

  @Test
  public void negative_recursive_fibonacci_is_correct() {
    var fibProgram = Source.create("nim", """
      def fib(n) {
          if n > -2 {
              return abs(n)
          }
          return fib(n + 1) + fib(n + 2)
      }
      fib(-20)
      """);
    var fibProgramValue = context.parse(fibProgram);
    assertEquals(6765, fibProgramValue.execute().asInt());
  }
}
