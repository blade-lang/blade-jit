package org.blade;

import org.openjdk.jmh.annotations.Benchmark;

public class SelfCounterBenchmark extends TruffleBenchmark {
  private static final int INPUT = 1_000_000;

  private static final String COUNTER_CLASS = """
    class Counter {
        @new() {
            self.count = 0
        }
        increment() {
            self.count = self.count + 1
        }
        getCount() {
            return self.count
        }
    }
    """;

  private static final String JS_COUNTER_CLASS = """
    class Counter {
        constructor() {
            this.count = 0;
        }
        increment() {
            this.count = this.count + 1;
        }
        getCount() {
            return this.count;
        }
    }
    """;

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

    context.eval("blade", COUNTER_CLASS);
    context.eval("blade", COUNT_WITH_SELF_IN_ITER);

    context.eval("js", JS_COUNTER_CLASS);
    context.eval("js", JS_COUNT_WITH_SELF_IN_ITER);
  }

  @Benchmark
  public int nim_eval() {
    return context.eval("blade", "countWithSelfInIterDirect(" + INPUT + ");").asInt();
  }

  @Benchmark
  public int js_eval() {
    return context.eval("js", "countWithSelfInIterDirect(" + INPUT + ");").asInt();
  }
}
