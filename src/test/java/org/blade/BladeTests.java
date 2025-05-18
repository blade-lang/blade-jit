package org.blade;

import org.junit.Test;
import org.junit.runner.RunWith;

//@RunWith(BladeTestRunner.class)
//@BladeTestSuite({"tests"})
public class BladeTests {
  public static void main(String[] args) throws Exception {
    BladeTestRunner.runInMain(BladeTestSuite.class, args);
  }

  /*
   * Our "mx unittest" command looks for methods that are annotated with @Test. By just defining
   * an empty method, this class gets included and the test suite is properly executed.
   */
  @Test
  public void unittest() {
  }
}
