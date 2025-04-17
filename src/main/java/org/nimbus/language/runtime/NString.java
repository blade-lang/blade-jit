package org.nimbus.language.runtime;

import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;

public final class NString {
  public final static TruffleString EMPTY = fromJavaString("");

  public static TruffleString fromJavaString(String s) {
    return TruffleString.fromJavaStringUncached(s, NimbusLanguage.ENCODING);
  }

  public static TruffleString fromLong(TruffleString.FromLongNode fromLongNode, long value) {
    return fromLongNode.execute(value, NimbusLanguage.ENCODING, true);
  }

  public static TruffleString concat(TruffleString.ConcatNode concatNode, TruffleString left, TruffleString right) {
    return concatNode.execute(left, right, NimbusLanguage.ENCODING, true);
  }

  public static TruffleString fromObject(Object object) {
    return fromJavaString(object.toString());
  }

  public static TruffleString fromObject(TruffleString.FromJavaStringNode fromJavaStringNode, Object object) {
    return fromJavaStringNode.execute(object.toString(), NimbusLanguage.ENCODING);
  }

  public static boolean equals(TruffleString a, TruffleString b, TruffleString.EqualNode equalNode) {
    return equalNode.execute(a, b, NimbusLanguage.ENCODING);
  }

  public static long length(TruffleString string, TruffleString.CodePointLengthNode lengthNode) {
    return lengthNode.execute(string, NimbusLanguage.ENCODING);
  }

  public static Object substring(TruffleString string, int startIndex, int length, TruffleString.SubstringNode substringNode) {
    return substringNode.execute(string, startIndex, length, NimbusLanguage.ENCODING, true);
  }

  public static long indexOf(TruffleString.IndexOfStringNode indexOfStringNode, TruffleString.CodePointLengthNode lengthNode,
                             TruffleString self, TruffleString other, int startIndex) {
    return indexOfStringNode.execute(
      self, other,
      startIndex,
      (int)length(self, lengthNode),
      NimbusLanguage.ENCODING
    );
  }

  public static boolean same(Object o1, Object o2) {
    return o1 == o2;
  }
}
