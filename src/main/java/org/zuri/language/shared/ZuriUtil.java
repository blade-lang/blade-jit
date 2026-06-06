package org.zuri.language.shared;

import com.oracle.truffle.api.CompilerDirectives;
import org.zuri.annotations.NAnnotationHelper;

public class ZuriUtil {
  @CompilerDirectives.TruffleBoundary
  public static String getObjectType(Object o) {
    String[] qualifiedName = o == null ? new String[]{"Unknown"} : NAnnotationHelper.getObjectName(o.getClass())
      .split("[.]");
    String name = qualifiedName[qualifiedName.length - 1];
    if (name.equals("TruffleString")) {
      name = "String";
    }
    return name;
  }
}
