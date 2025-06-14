package org.blade;

import com.oracle.truffle.api.debug.*;
import com.oracle.truffle.tck.DebuggerTester;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Before;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DebuggerTest {
  private static final String FIB_ITER = """
    class Fib {
        fib(unused, num) {
            var n1 = 0, n2 = 1
            if num > 1 {
                var i = 1
                while i < num {
                    const next = n1 + n2
                    n1 = n2
                    n2 = next
                    i = i + 1
                }
                return n2
            } else {
                return abs(num)
            }
        }
    }
    const fibM1 = new Fib().fib('unused-1', -1)
    var fib2
    fib2 = new Fib().fib('unused2', 2, 'superfluous2')
    fibM1 + fib2
    """;

  private DebuggerTester debuggerTester;

  @Before
  public void setUp() {
    this.debuggerTester = new DebuggerTester();
  }

  @After
  public void tearDown() {
    this.debuggerTester.close();
  }

  //  @Test
  public void step_over_global_var_decl_and_into_func_call() {
    Source source = Source.create("blade", FIB_ITER);

    try (DebuggerSession debuggerSession = this.debuggerTester.startSession()) {
      debuggerSession.suspendNextExecution();
      this.debuggerTester.startEval(source);
      this.debuggerTester.expectSuspended(event -> {
        assertState(event, 18, SuspendAnchor.BEFORE, "const fibM1 = new Fib().fib('unused-1', -1)\n");
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(event, 19, SuspendAnchor.BEFORE, "var fib2\n");
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(event, 20, SuspendAnchor.BEFORE, "fib2 = new Fib().fib('unused2', 2, 'superfluous2')\n");
        event.prepareStepInto(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 3, SuspendAnchor.BEFORE, "var n1 = 0, n2 = 1",
          List.of(Map.of("num", "2", "self", "[object Object]", "n1", "nil", "n2", "nil"))
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 3, SuspendAnchor.BEFORE, "var n1 = 0, n2 = 1",
          List.of(Map.of("num", "2", "this", "[object Object]", "n1", "0", "n2", "nil"))
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 4, SuspendAnchor.BEFORE, """
            if (num > 1) {
                var i = 1
                while i < num {
                    const next = n1 + n2
                    n1 = n2
                    n2 = next
                    i = i + 1
                }
                return n2
            } else {
                return abs(num)
            }""",
          List.of(Map.of("num", "2", "this", "[object Object]", "n1", "0", "n2", "1"))
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 5, SuspendAnchor.BEFORE, "var i = 1",
          List.of(
            Map.of("num", "2", "this", "[object Object]", "n1", "0", "n2", "1"),
            Map.of("i", "nil")
          )
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 6, SuspendAnchor.BEFORE, """
            while (i < num) {
                const next = n1 + n2
                n1 = n2
                n2 = next
                i = i + 1
            }""",
          List.of(
            Map.of("num", "2", "this", "[object Object]", "n1", "0", "n2", "1"),
            Map.of("i", "1")
          )
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 7, SuspendAnchor.BEFORE, "const next = n1 + n2",
          List.of(
            Map.of("num", "2", "this", "[object Object]", "n1", "0", "n2", "1"),
            Map.of("i", "1"),
            Map.of("next", "nil")
          )
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 8, SuspendAnchor.BEFORE, "n1 = n2",
          List.of(
            Map.of("num", "2", "this", "[object Object]", "n1", "0", "n2", "1"),
            Map.of("i", "1"),
            Map.of("next", "1")
          )
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 9, SuspendAnchor.BEFORE, "n2 = next",
          List.of(
            Map.of("num", "2", "this", "[object Object]", "n1", "1", "n2", "1"),
            Map.of("i", "1"),
            Map.of("next", "1")
          )
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 10, SuspendAnchor.BEFORE, "i = i + 1",
          List.of(
            Map.of("num", "2", "this", "[object Object]", "n1", "1", "n2", "1"),
            Map.of("i", "1"),
            Map.of("next", "1")
          )
        );
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 12, SuspendAnchor.BEFORE, "return n2",
          List.of(
            Map.of("num", "2", "this", "[object Object]", "n1", "1", "n2", "1"),
            Map.of("i", "2")
          )
        );
        event.prepareStepOut(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(event, 20, SuspendAnchor.AFTER, "fib2 = new Fib().fib('unused2', 2, 'superfluous2')");
        event.prepareStepInto(1);
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(event, 21, SuspendAnchor.BEFORE, "fibM1 + fib2");
        event.prepareStepOver(1);
      });
      this.debuggerTester.expectDone();
    }
  }

  //  @Test
  public void setting_breakpoint_suspends_execution() {
    Source source = Source.create("blade", FIB_ITER);

    try (DebuggerSession debuggerSession = this.debuggerTester.startSession()) {
      debuggerSession.suspendNextExecution();
      debuggerSession.install(Breakpoint.newBuilder(source.getURI())
        .lineIs(8)
        .build());
      this.debuggerTester.startEval(source);
      this.debuggerTester.expectSuspended(event -> {
        assertState(event, 18, SuspendAnchor.BEFORE, "const fibM1 = new Fib().fib('unused-1', -1)");
        event.prepareContinue();
      });
      this.debuggerTester.expectSuspended(event -> {
        assertState(
          event, "fib", 8, SuspendAnchor.BEFORE, "n1 = n2",
          List.of(
            Map.of("num", "2", "this", "[object Object]", "n1", "0", "n2", "1"),
            Map.of("i", "1"),
            Map.of("next", "1")
          )
        );
        event.prepareContinue();
      });

      this.debuggerTester.expectDone();
    }
  }

  private static void assertState(
    SuspendedEvent suspendedEvent, int expectedLineNumber,
    SuspendAnchor suspendAnchor, String expectedCode) {
    assertState(suspendedEvent, "@.script", expectedLineNumber, suspendAnchor, expectedCode, List.of(Map.of()));
  }

  private static void assertState(
    SuspendedEvent suspendedEvent, String frameName, int expectedLineNumber,
    SuspendAnchor suspendAnchor, String expectedCode, List<Map<String, String>> expectedFrameValues) {
    DebugStackFrame frame = suspendedEvent.getTopStackFrame();
    assertEquals(frameName, frame.getName());

    assertEquals(expectedLineNumber, suspendedEvent.getSourceSection().getStartLine());
    assertEquals(suspendAnchor, suspendedEvent.getSuspendAnchor());
    assertEquals(expectedCode, suspendedEvent.getSourceSection().getCharacters().toString());

    assertEquals(expectedFrameValues, scopeValues(frame.getScope()));
  }

  private static List<Map<String, String>> scopeValues(DebugScope scope) {
    List<Map<String, String>> ret = new LinkedList<>();
    DebugScope currentScope = scope;
    while (currentScope != null) {
      Map<String, String> values = new HashMap<>();
      ret.addFirst(values);
      for (DebugValue value : currentScope.getDeclaredValues()) {
        values.put(value.getName(), value.toDisplayString());
      }
      currentScope = currentScope.getParent();
    }
    return ret;
  }
}
