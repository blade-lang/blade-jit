package org.blade.language.shared;

import com.oracle.truffle.api.CompilerDirectives;
import org.blade.annotations.NAnnotationHelper;

public class BladeUtil {
  @CompilerDirectives.TruffleBoundary
  public static String getObjectType(Object o) {
    String[] qualifiedName = o == null ? new String[]{"Unknown"} : NAnnotationHelper.getObjectName(o.getClass()).split("[.]");
    String name = qualifiedName[qualifiedName.length - 1];
    if(name.equals("TruffleString")) {
      name = "String";
    }
    return name;
  }
}
