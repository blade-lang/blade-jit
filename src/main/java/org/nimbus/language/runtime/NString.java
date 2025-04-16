package org.nimbus.language.runtime;

import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;

public final class NString {
  public final static TruffleString EMPTY = fromJavaString("");

  public static TruffleString fromJavaString(String s) {
    return TruffleString.fromJavaStringUncached(s, NimbusLanguage.ENCODING);
  }
}
