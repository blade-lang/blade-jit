package org.nimbus;

import org.graalvm.polyglot.*;
import org.nimbus.language.NimbusLanguage;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
  public static void main(String[] args) throws IOException {
//    System.out.println(ProcessHandle.current().pid());

    Source source;
    Map<String, String> options = new HashMap<>();
    String file = null;
    for (String arg : args) {
      if (!parseOption(options, arg)) {
        if (file == null) {
          file = arg;
        }
      }
    }

    Engine engine = Engine.newBuilder()
//      .logHandler(new OutputStream() {
//        @Override
//        public void write(int b) throws IOException {
//          // DO NOTHING...
//        }
//      })
      .allowExperimentalOptions(true)
      .options(options)
      .build();

    Context defaultContext = Context.newBuilder(NimbusLanguage.ID)
      .engine(engine)
      .allowAllAccess(true)
      .build();


    if (file != null) {
      System.exit(runSource(defaultContext, getSource(file), false));
    } else {
      while (true) {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        System.out.print("%> ");

        String line = reader.readLine();
        if(line.equals(".exit")) {
          break;
        }

        runSource(defaultContext, createSource(line), true);
      }
    }
  }

  private static Source getSource(String path) throws IOException {
    return Source.newBuilder(NimbusLanguage.ID, new File(path)).build();
  }

  private static Source createSource(String content) throws IOException {
    return Source.newBuilder(NimbusLanguage.ID, content, "<script>").build();
  }

  private static int runSource(Context context, Source source, boolean isRepl) {
    try {
      try {
        Value value = context.eval(source);
        if (isRepl) {
          System.out.println(value);
        }

        return 0;
      } catch (PolyglotException e) {
//        if (e.isInternalError()) {
          // for internal errors we print the full stack trace
          e.printStackTrace();
//        } else {
//          System.err.println(e.getMessage());
//        }
      }
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
    }

    return 1;
  }

  private static boolean parseOption(Map<String, String> options, String arg) {
    if (arg.length() <= 2 || !arg.startsWith("--")) {
      return false;
    }
    int eqIdx = arg.indexOf('=');
    String key;
    String value;
    if (eqIdx < 0) {
      key = arg.substring(2);
      value = null;
    } else {
      key = arg.substring(2, eqIdx);
      value = arg.substring(eqIdx + 1);
    }

    if (value == null) {
      value = "true";
    }
    int index = key.indexOf('.');
    String group = key;
    if (index >= 0) {
      group = group.substring(0, index);
    }
    options.put(key, value);
    return true;
  }
}
