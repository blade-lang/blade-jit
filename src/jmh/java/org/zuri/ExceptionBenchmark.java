package org.zuri;

import org.openjdk.jmh.annotations.Benchmark;

public class ExceptionBenchmark extends TruffleBenchmark {
  private static final int INPUT = 1_000_000;

  private static final String ZURI_SOURCE = """
    class Countdown {
        @new(start) {
            self.count = start
        }
        decrement() {
            if self.count <= 0 {
                raise new Error('countdown has completed')
            }
            self.count = self.count - 1
        }
    }
    def countdown(n) {
        const countdown = new Countdown(n)
        var ret = 0
        iter ;; {
            try {
                countdown.decrement()
                ret = ret + 1
            } catch e {
                break
            }
        }
        return ret
    }
    """;

  private static final String JS_SOURCE = """
    class Countdown {
        constructor(start) {
            this.count = start;
        }
        decrement() {
            if (this.count <= 0) {
                throw new Error('countdown has completed');
            }
            this.count = this.count - 1;
        }
    }
    function countdown(n) {
        const countdown = new Countdown(n);
        let ret = 0;
        for (;;) {
            try {
                countdown.decrement();
                ret = ret + 1;
            } catch (e) {
                break;
            }
        }
        return ret;
    }
    """;

  @Override
  public void setup() {
    super.setup();

    context.eval("zuri", ZURI_SOURCE);

    context.eval("js", JS_SOURCE);
  }

  @Benchmark
  public int zuri_eval() {
    context.eval("zuri", "countdown(" + INPUT + ");");
    return 1;
  }

  @Benchmark
  public int js_eval() {
    context.eval("js", "countdown(" + INPUT + ");");
    return 1;
  }
}
