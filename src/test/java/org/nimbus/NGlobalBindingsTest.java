package org.nimbus;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class NGlobalBindingsTest {
  @Test
  public void surfaces_global_bindings() {
    var context = Context.create();
    context.eval("nim",
      """
        var a = 1
        var b = 2 + 3
        const c = 4.0"""
    );

    var globalBindings = context.getBindings("nim");
    assertFalse(globalBindings.isNull());
    assertTrue(globalBindings.hasMembers());
    assertTrue(globalBindings.hasMember("a"));
    assertTrue(globalBindings.getMemberKeys().containsAll(Set.of("abs", "Object", "time", "a", "b", "c")));

    Value b = globalBindings.getMember("b");
    assertEquals(5, b.asInt());
  }
}
