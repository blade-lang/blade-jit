package org.nimbus;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
    assertEquals(Set.of("abs", "a", "b", "c"), globalBindings.getMemberKeys());

    Value b = globalBindings.getMember("b");
    assertEquals(5, b.asInt());
  }
}
