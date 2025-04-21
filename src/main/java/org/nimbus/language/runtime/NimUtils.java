package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;

public class NimUtils {
  
  @CompilerDirectives.TruffleBoundary
  public static void print(Object object) {
    System.out.println(object);
  }
}
