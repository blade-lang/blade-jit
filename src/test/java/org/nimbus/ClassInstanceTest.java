package org.nimbus;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassInstanceTest {

  @Test
  public void test_class_instance_works_in_loop() {
    var value = Context.create().eval("nim", """
    class Counter {
        Counter() {
            self.count = 0
        }
        increment() {
            self.count = self.count + 1
        }
        getCount() {
            return self.count
        }
    }
    def countWithSelfInIterDirect(n) {
        const counter = new Counter()
        iter var i = 0; i < n; i = i + 1 {
            counter.increment()
        }
        return counter.getCount()
    }
    
    countWithSelfInIterDirect(1000)""");

    assertEquals(1000, value.asInt());
  }
}
