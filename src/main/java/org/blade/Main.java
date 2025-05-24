package org.blade;

import org.graalvm.polyglot.*;
import org.blade.language.BladeLanguage;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Main {
  private static final String SHOW_PID = "show-pid";

  public static void main(String[] args) throws IOException {

    Map<String, String> options = new HashMap<>();
    String file = null;
    for (String arg : args) {
      if (!parseOption(options, arg)) {
        if (file == null) {
          file = arg;
        }
      }
    }

    if(options.containsKey(SHOW_PID)) {
      System.out.println(ProcessHandle.current().pid());
      options.remove(SHOW_PID);
    }

    try(
      Context defaultContext = Context.newBuilder(BladeLanguage.ID)
        .in(System.in)
        .out(System.out)
        .err(System.err)
        .allowExperimentalOptions(true)
        .allowAllAccess(true)
        .options(options)
//        .logHandler(new OutputStream(){
//          @Override
//          public void write(int b) throws IOException {
//
//          }
//        })
        .build()
    ) {
      if (file != null) {
        try {
          System.exit(runSource(defaultContext, getSource(file), false));
        } catch (NoSuchFileException ex) {
          System.err.printf("""
              (Blade):
                Launch aborted for %s
                Reason: No such file or directory%n""", file);
        }
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
  }

  private static Source getSource(String path) throws IOException {
    return Source.newBuilder(BladeLanguage.ID, new File(path)).build();
  }

  private static Source createSource(String content) throws IOException {
    return Source.newBuilder(BladeLanguage.ID, content, "<script>").build();
  }

  private static int runSource(Context context, Source source, boolean isRepl) {
    try {
      try {
        Value value = context.eval(source);
        if (isRepl) {
          if(value.isString()) {
            System.out.println("'" + value.asString() + "'");
          } else {
            System.out.println(value);
          }
        }

        return 0;
      } catch (PolyglotException e) {
        if (e.isInternalError()) {
//           for internal errors, we print the full stack trace for now
          // TODO: Robust handling...
          e.printStackTrace();
        } else {
          printStackTrace(e.getMessage(), e.getPolyglotStackTrace().iterator());
        }
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

    if(group.equals("blade") || key.equals("inspect")) {
      options.put(key, value);
    }
    return true;
  }

  private static void printStackTrace(String message, Iterator<PolyglotException.StackFrame> elements) {
    System.err.println(message);
    while(elements.hasNext()) {
      PolyglotException.StackFrame frame = elements.next();
      if(frame.isGuestFrame()) {
        SourceSection sourceSection = frame.getSourceLocation();
        if(sourceSection != null) {
          String funcName = frame.getRootName();

          String fileName = sourceSection.getSource().getName();
          String filePath = sourceSection.getSource().getPath();

          int lineNo = sourceSection.getStartLine();

          if(lineNo > -1) {
            System.err.println("    at "+(filePath == null ? fileName : filePath)+":"+lineNo+" -> "+funcName+"()");
          }
        }
      }
    }
  }
}
