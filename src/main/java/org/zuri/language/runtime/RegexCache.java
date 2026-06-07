package org.zuri.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.regex.RegexObject;
import org.zuri.language.builtins.StringMethods;

import java.util.concurrent.ConcurrentHashMap;

public class RegexCache {
  private final TruffleLanguage.Env env;
  private final ConcurrentHashMap<String, RegexObject> cache = new ConcurrentHashMap<>();

  public RegexCache(TruffleLanguage.Env env) {
    this.env = env;
  }

  public record RegexSeparation(String pattern, String flag) {
  }

  @CompilerDirectives.TruffleBoundary
  public static RegexSeparation separateRegex(String inputStr) {
    if(inputStr.isEmpty()) {
      return new RegexSeparation("", "");
    }

    char delimiter = inputStr.charAt(0);
    int endDelimiter = inputStr.lastIndexOf(delimiter);

    // Handle edge cases such as "//" and "~~". They're empty regexes!
    if (endDelimiter == delimiter + 1) {
      return new RegexSeparation("", "");
    }

    // Also handle cases like "aa", "11"
    if (
      Character.isAlphabetic(delimiter) || Character.isDigit(delimiter)
        || Character.isSpaceChar(delimiter) || delimiter == '\\' || delimiter == '\0'
    ) {
      return new RegexSeparation(inputStr, "");
    }

    String flag = "";
    if (endDelimiter > 0) {
      if(endDelimiter != inputStr.length() - 1) {
        flag = inputStr.substring(endDelimiter + 1);
      }

      inputStr = inputStr.substring(1, endDelimiter);
    }

    return new RegexSeparation(inputStr, flag);
  }

  /**
   * Returns a compiled TRegex object for the given pattern + flags.
   * Compiled objects are cached by "pattern|flags" key.
   */
  @CompilerDirectives.TruffleBoundary
  public RegexObject compile(String pattern, String flags, InteropLibrary interop) {
    String key = pattern + "|" + flags;
    return cache.computeIfAbsent(key, k -> {
      try {
        String regexSrc = "Flavor=ECMAScript/" + pattern + "/" + flags;
        Source src = Source.newBuilder("regex", regexSrc, "zuri-regex")
          .internal(true)
          .build();

        Object result = env.parseInternal(src).call();

        if(interop.isNull(result)) {
          throw new ZuriRuntimeError("Invalid regex pattern -- " + pattern);
        }

        return (RegexObject) result;
      } catch (Exception e) {
        throw new ZuriRuntimeError("Invalid regex pattern -- " + pattern + " --> " + e.getMessage());
      }
    });
  }

  @CompilerDirectives.TruffleBoundary
  public RegexObject compile(String pattern, String flags) {
    return compile(pattern, flags, InteropLibrary.getUncached());
  }

  @CompilerDirectives.TruffleBoundary
  public RegexObject compile(String pattern, InteropLibrary interop) {
    RegexSeparation separation = separateRegex(pattern);

    return compile(separation.pattern, separation.flag, interop);
  }

  @CompilerDirectives.TruffleBoundary
  public RegexObject compile(String pattern) {
    return compile(pattern, InteropLibrary.getUncached());
  }
}
