package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;

public class BladeUtils {
  
  @CompilerDirectives.TruffleBoundary
  public static void print(Object object) {
    System.out.println(object);
  }
}
