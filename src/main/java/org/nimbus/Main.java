package org.nimbus;

import org.graalvm.polyglot.*;
import org.nimbus.language.NimbusLanguage;

import java.io.*;

public class Main {
  public static void main(String[] args) throws IOException {
//    System.out.println(ProcessHandle.current().pid());
    if (args.length > 1) {
      System.err.println("Usage: " + NimbusLanguage.ID + " [file]");
      System.exit(64);
    } else if (args.length == 1) {
      System.exit(runSource(getSource(args[0]), false));
    } else {
      while (true) {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        System.out.print("%> ");

        String line = reader.readLine();
        if(line.equals(".exit")) {
          break;
        }

        runSource(createSource(line), true);
      }
    }
  }

  private static Source getSource(String path) throws IOException {
    return Source.newBuilder(NimbusLanguage.ID, new File(path)).build();
  }

  private static Source createSource(String content) throws IOException {
    return Source.newBuilder(NimbusLanguage.ID, content, "<script>").build();
  }

  private static final Engine ENGINE = Engine.newBuilder()
    .logHandler(new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        // DO NOTHING...
      }
    })
//    .option("engine.WarnInterpreterOnly", "false")
    .build();

  private static final Context defaultContext = Context.newBuilder(NimbusLanguage.ID)
    .engine(ENGINE)
    .allowAllAccess(true)
    .build();

  private static int runSource(Source source, boolean isRepl) {
    try {
      try {
        Value value = defaultContext.eval(source);
        if (isRepl) {
          System.out.println(value);
        }

        return 0;
      } catch (PolyglotException e) {
        if (e.isInternalError()) {
          // for internal errors we print the full stack trace
          e.printStackTrace();
        } else {
          System.err.println(e.getMessage());
        }
      }
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
    }

    return 1;
  }
}
