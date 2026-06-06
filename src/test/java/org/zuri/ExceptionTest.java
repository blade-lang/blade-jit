package org.zuri;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertThrows;

public class ExceptionTest {
  @Test
  public void raises() throws IOException {
    assertThrows(
      PolyglotException.class, () -> {
        Source source = Source
          .newBuilder("zuri", new File("src/test/resources/exceptions.zu"))
          .build();
        Context context = Context.create();
        context.eval(source);
        context.close();
      }
    );
  }
}
