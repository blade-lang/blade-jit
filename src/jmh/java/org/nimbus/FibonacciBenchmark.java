package org.nimbus;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class FibonacciBenchmark extends TruffleBenchmark {
  private static final String REM_FIBONACCI = """
    def fib(n) {
        if n < 2 {
            return 1
        }
        return fib(n - 1) + fib(n - 2)
    }
    """;

  private static final String SL_FIBONACCI = "\n" +
    "function fib(n) { " +
    "    if (n < 2) { " +
    "        return 1; " +
    "    } " +
    "    return fib(n - 1) + fib(n - 2); " +
    "}";

  public static int fibonacciJava(int n) {
    return n < 2
      ? 1
      : fibonacciJava(n - 1) + fibonacciJava(n - 2);
  }

  @Override
  public void setup() {
    super.setup();

    context.eval("nim", REM_FIBONACCI);
    context.eval("sl", SL_FIBONACCI);
    context.eval("js", SL_FIBONACCI);
  }

//  @Fork(jvmArgsPrepend = {
//    "-Dgraal.Dump=Truffle:1",
//    "-Dgraal.PrintGraph=File"
//  })
  @Benchmark
  public int nim_eval() {
    return context.eval("nim", "fib(20)").asInt();
  }

  @Benchmark
  public int java_eval() {
    return fibonacciJava(20);
  }

  @Fork(value = 1, jvmArgsAppend = {
    "--add-exports",
    "org.graalvm.truffle/com.oracle.truffle.api.staticobject=ALL-UNNAMED",
  })
  @Benchmark
  public int sl_eval() {
    return context.eval("sl", "function main() { " +
      "    return fib(20); " +
      "}").asInt();
  }

  @Fork(value = 1, jvmArgsAppend = {
    "--add-exports",
    "org.graalvm.truffle/com.oracle.truffle.api.staticobject=ALL-UNNAMED",
  })
  @Benchmark
  public int js_eval() {
    return context.eval("js", "fib(20)").asInt();
  }
}
