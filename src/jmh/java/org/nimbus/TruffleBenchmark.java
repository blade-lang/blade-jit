package org.nimbus;

import org.graalvm.polyglot.Context;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 15, time = 1)
@Fork(value = 1, jvmArgsAppend = {
//  "-Xms2G", "-Xmx2G",
  "-Dgraalvm.locatorDisabled=true",
  "--enable-native-access=ALL-UNNAMED",
  "--sun-misc-unsafe-memory-access=allow",
})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class TruffleBenchmark {
  protected Context context;

  @Setup
  public void setup() {
    context = Context.create();
  }

  @TearDown
  public void tearDown() {
    context.close();
  }
}
