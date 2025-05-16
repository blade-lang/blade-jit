package org.blade;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClassTest {
  private Context context;

  @Before
  public void setup() {
    context = Context.create();
  }

  @After
  public void tearDown() {
    context.close();
  }

  @Test
  public void class_declaration_creates_object() {
    var result = context.eval("blade", "class A {}\n" +
      "A");

    assertEquals("<class A>", result.toString());
    assertTrue(result.hasMembers());
  }

  @Test
  public void class_can_be_instantiated() {
    var result = context.eval("blade", """
      class A {
          a() {
              return 'A.a'
          }
      }
      new A()""");

    assertTrue(result.hasMembers());
//    assertEquals(Set.of("a"), result.getMemberKeys());
    assertTrue(result.hasMember("a"));
    var methodA = result.getMember("a");
    assertTrue(methodA.canExecute());
    assertEquals("A.a", methodA.execute().asString());
  }

  @Test
  public void methods_can_be_called_on_class_instances() {
    var result = context.eval("blade", """
      class A {
          a() {
              return 'A.a'
          }
      }
      new A().a();""");

    assertEquals("A.a", result.asString());
  }

  @Test
  public void classes_can_be_reassigned() {
    var result = context.eval("blade", """
      class A { }
      A = 5
      A""");

    assertEquals(5, result.asInt());
  }

  @Test
  public void arguments_passed_to_new_are_evaluated() {
    var result = context.eval("blade", """
      class Class { }
      var l = 3
      new Class(l = 5)
      l""");

    assertEquals(5, result.asInt());
  }

  @Test
  public void duplicate_methods_override_previous_ones() {
    var result = context.eval("blade", """
      class Class {
         c() { return 1 }
         c() { return 2 }
      }
      new Class().c()""");

    assertEquals(2, result.asInt());
  }

  @Test
  public void class_instances_can_be_used_as_function_arguments() {
    context.eval("blade", """
      class M {
          m(a) {
              return a + 1
          }
      }
      def invokeM(target, argument) {
          return target.m(argument)
      }
      var m = new M()"""
    );
    
    var bindings = context.getBindings("blade");
    var m = bindings.getMember("m");
    var invokeM = bindings.getMember("invokeM");

    assertEquals(14, invokeM.execute(m, 13).asInt());
  }

  @Test
  public void unknown_class_instance_member_read_through_GraalVM_interop_returns_null() {
    var obj = context.eval("blade", """
      class A { }
      new A();""");
    assertTrue(obj.hasMembers());
    var doesNotExist = obj.getMember("doesNotExist");
    assertNull(doesNotExist);
  }

  @Test
  public void benchmark_with_alloc_inside_loop_returns_input() {
    var result = context.eval("blade", """
      class Adder {
          add(a, b) {
              return a + b
          }
      }
      def countForMethodPropAllocInsideLoop(n) {
          var ret = 0
          iter var i = 0; i < n; i = i + 1 {
              ret = new Adder().add(ret, 1)
          }
          return ret
      }
      countForMethodPropAllocInsideLoop(1000)"""
    );

    assertEquals(1_000, result.asInt());
  }

  @Test
  public void benchmark_with_alloc_outside_loop_returns_input() {
    var result = context.eval("blade", """
      class Adder {
          add(a, b) {
              return a + b;
          }
      }
      def countForMethodPropAllocOutsideLoop(n) {
          var ret = 0;
          var adder = new Adder();
          iter var i = 0; i < n; i = i + 1 {
              ret = adder['add'](ret, 1);
          }
          return ret;
      }
      countForMethodPropAllocOutsideLoop(1000)"""
    );

    assertEquals(1_000, result.asInt());
  }

  @Test
  public void duplicate_class_declarations_are_an_error() {
    try {
      context.eval("blade",
        "class A { }\n" +
        "class A { }"
      );
      fail("expected PolyglotException to be thrown");
    } catch (PolyglotException e) {
      e.printStackTrace();
      assertTrue(e.isGuestException());
      assertFalse(e.isInternalError());
      assertEquals("Error: 'A' already declared in this scope", e.getMessage());
    }
  }

  @Test
  public void new_with_non_class_is_an_error() {
    try {
      context.eval("blade",
        "new 3();");
      fail("expected PolyglotException to be thrown");
    } catch (PolyglotException e) {
      assertTrue(e.isGuestException());
      assertFalse(e.isInternalError());
      assertEquals("Error: '3' is not a constructor", e.getMessage());
    }
  }
}
