package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import org.blade.language.BladeLanguage;

import java.util.List;

public final class BString {
  public final static TruffleString EMPTY = fromJavaString("");

  public static TruffleString fromJavaString(String s) {
    return TruffleString.fromJavaStringUncached(s, BladeLanguage.ENCODING);
  }

  public static TruffleString fromLong(TruffleString.FromLongNode fromLongNode, long value) {
    return fromLongNode.execute(value, BladeLanguage.ENCODING, true);
  }

  public static TruffleString concat(TruffleString.ConcatNode concatNode, TruffleString left, TruffleString right) {
    return concatNode.execute(left, right, BladeLanguage.ENCODING, true);
  }

  public static TruffleString fromObject(Object object) {
    return fromJavaString(toString(object));
  }

  public static TruffleString fromObject(TruffleString.FromJavaStringNode fromJavaStringNode, Object object) {
    return fromJavaStringNode.execute(toString(object), BladeLanguage.ENCODING);
  }

  public static TruffleString fromCodePoint(TruffleString.FromCodePointNode fromCodePointNode, int codepoint) {
    return fromCodePointNode.execute(codepoint, BladeLanguage.ENCODING);
  }

  public static int toCodePoint(TruffleString string, TruffleString.CodePointAtIndexNode codePointNode, int index) {
    return codePointNode.execute(string, index, BladeLanguage.ENCODING);
  }

  @CompilerDirectives.TruffleBoundary
  public static Object fromObject(InteropLibrary interopLibrary, Object object) {
    return interopLibrary.toDisplayString(object);
  }

  public static boolean equals(TruffleString a, TruffleString b, TruffleString.EqualNode equalNode) {
    return equalNode.execute(a, b, BladeLanguage.ENCODING);
  }

  public static long length(TruffleString string, TruffleString.CodePointLengthNode lengthNode) {
    return lengthNode.execute(string, BladeLanguage.ENCODING);
  }

  public static int intLength(TruffleString string, TruffleString.CodePointLengthNode lengthNode) {
    return lengthNode.execute(string, BladeLanguage.ENCODING);
  }

  public static Object substring(TruffleString string, int startIndex, int length, TruffleString.SubstringNode substringNode) {
    return substringNode.execute(string, startIndex, length, BladeLanguage.ENCODING, true);
  }

  public static long indexOf(TruffleString.IndexOfStringNode indexOfStringNode, TruffleString.CodePointLengthNode lengthNode,
                             TruffleString self, TruffleString other, int startIndex) {
    return indexOfStringNode.execute(
      self, other,
      startIndex,
      (int)length(self, lengthNode),
      BladeLanguage.ENCODING
    );
  }

  public static long indexOfCodePoint(TruffleString.IndexOfCodePointNode indexOfNode,
                                      TruffleString self, int codePoint, int length) {
    return indexOfCodePoint(indexOfNode, self, codePoint, length, 0);
  }

  public static long indexOfCodePoint(TruffleString.IndexOfCodePointNode indexOfNode,
                                      TruffleString self, int codePoint, int length, int startIndex) {
    return indexOfNode.execute(
      self,
      codePoint,
      startIndex,
      length,
      BladeLanguage.ENCODING
    );
  }

  public static boolean same(Object o1, Object o2) {
    return o1 == o2;
  }

  @CompilerDirectives.TruffleBoundary
  public static String toString(Object object) {
    if(object == null) return "nil";
    return object.toString();
  }

  public static String tryToString(Object object) {
    return object instanceof String
      ? (String) object
      : toString(object);
  }

  @CompilerDirectives.TruffleBoundary
  public static String toUpper(String string) {
    return string.toUpperCase();
  }

  @CompilerDirectives.TruffleBoundary
  public static String toLower(String string) {
    return string.toLowerCase();
  }

  @CompilerDirectives.TruffleBoundary
  public static String join(String joinString, List<String> items) {
    return String.join(joinString, items);
  }

  @CompilerDirectives.TruffleBoundary
  public static String format(String format, Object ...args) {
    return String.format(format, args);
  }

  @CompilerDirectives.TruffleBoundary
  @ExplodeLoop
  public static String concatString(String original, Object ...others) {
    StringBuilder builder = new StringBuilder();
    builder.append(original);

    for(Object o : others) {
      builder.append(o.toString());
    }

    return builder.toString();
  }

  public static TruffleStringBuilder builder() {
    return TruffleStringBuilder.create(BladeLanguage.ENCODING);
  }
}
