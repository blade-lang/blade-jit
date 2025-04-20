package org.nimbus;

import org.openjdk.jmh.annotations.Benchmark;

public class ClassCounterBenchmark extends TruffleBenchmark {
  private static final int INPUT = 1_000_000;

  private static final String COUNTER_CLASS = """
    class Base < Object {
      @new() {
          parent()
          self.count = 0
      }
      increment() {
          self.count = self.count + 1
      }
      getCount() {
          return self.count
      }
    }
    class LowerMiddle < Base {
    }
    class UpperMiddle < LowerMiddle {
      @new() {
          parent()
      }
      increment() {
          return parent.increment()
      }
      getCount() {
          return parent.getCount()
      }
    }
    class Counter < UpperMiddle {
    }
    """;

  private static final String JS_COUNTER_CLASS = """
    
    class Base extends Object {
        constructor() {
            super();
            this.count = 0;
        }
        increment() {
            this.count = this.count + 1;
        }
        getCount() {
            return this.count;
        }
    }
    class LowerMiddle extends Base {
    }
    class UpperMiddle extends LowerMiddle {
        constructor() {
            super();
        }
        increment() {
            return super.increment();
        }
        getCount() {
            return super.getCount();
        }
    }
    class Counter extends UpperMiddle {
    }
    """;;

  private static final String COUNT_WITH_SELF_IN_ITER = """
    def countWithSelfInIterDirect(n) {
        const counter = new Counter()
        iter (var i = 0; i < n; i++) {
            counter.increment()
        }
        return counter.getCount()
    }""";

  private static final String JS_COUNT_WITH_SELF_IN_ITER = """
    function countWithSelfInIterDirect(n) {
        const counter = new Counter();
        for (var i = 0; i < n; i++) {
            counter.increment();
        }
        return counter.getCount();
    }""";

  @Override
  public void setup() {
    super.setup();

    context.eval("nim", COUNTER_CLASS);
    context.eval("nim", COUNT_WITH_SELF_IN_ITER);

    context.eval("js", JS_COUNTER_CLASS);
    context.eval("js", JS_COUNT_WITH_SELF_IN_ITER);
  }

  @Benchmark
  public int nim_eval() {
    return context.eval("nim", "countWithSelfInIterDirect(" + INPUT + ");").asInt();
  }

  @Benchmark
  public int js_eval() {
    return context.eval("js", "countWithSelfInIterDirect(" + INPUT + ");").asInt();
  }
}
