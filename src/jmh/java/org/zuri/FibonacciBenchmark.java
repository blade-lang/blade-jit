package org.zuri;

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

    context.eval("zuri", REM_FIBONACCI);
    context.eval("sl", SL_FIBONACCI);
    context.eval("js", SL_FIBONACCI);
  }

  //  @Fork(jvmArgsPrepend = {
//    "-Dgraal.Dump=Truffle:1",
//    "-Dgraal.PrintGraph=File"
//  })
  @Benchmark
  @Fork(value = 1, jvmArgsAppend = {
    "--add-exports",
    "org.graalvm.truffle/com.oracle.truffle.api.staticobject=ALL-UNNAMED",
  })
  public int zuri_eval() {
    context.eval("zuri", "fib(20)");
    return 0;
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
    context.eval(
      "sl", "function main() { " +
        "    return fib(20); " +
        "}"
    );
    return 0;
  }

  @Fork(value = 1, jvmArgsAppend = {
    "--add-exports",
    "org.graalvm.truffle/com.oracle.truffle.api.staticobject=ALL-UNNAMED",
  })
  @Benchmark
  public int js_eval() {
    context.eval("js", "fib(20)");
    return 0;
  }
}
