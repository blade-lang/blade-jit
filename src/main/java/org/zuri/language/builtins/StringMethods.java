package org.zuri.language.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.result.RegexResult;
import org.zuri.language.BaseBuiltinDeclaration;
import org.zuri.language.ZuriLanguage;
import org.zuri.language.nodes.common.NToStringNode;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.language.runtime.*;
import org.zuri.language.shared.BuiltinClassesModel;
import org.zuri.utility.RegulatedMap;

import java.util.ArrayList;
import java.util.List;

public class StringMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {
      {
        add("@key", false, StringMethodsFactory.NKeyDecoratorNodeFactory.getInstance());
        add("@value", false, StringMethodsFactory.NValueDecoratorNodeFactory.getInstance());
        add("length", false, StringMethodsFactory.NLengthMethodNodeFactory.getInstance());
        add("upper", false, StringMethodsFactory.NUpperMethodNodeFactory.getInstance());
        add("lower", false, StringMethodsFactory.NLowerMethodNodeFactory.getInstance());
        add("is_alpha", false, StringMethodsFactory.NIsAlphaMethodNodeFactory.getInstance());
        add("is_alnum", false, StringMethodsFactory.NIsAlNumMethodNodeFactory.getInstance());
        add("is_number", false, StringMethodsFactory.NIsNumberMethodNodeFactory.getInstance());
        add("is_lower", false, StringMethodsFactory.NIsLowerMethodNodeFactory.getInstance());
        add("is_upper", false, StringMethodsFactory.NIsUpperMethodNodeFactory.getInstance());
        add("is_space", false, StringMethodsFactory.NIsSpaceMethodNodeFactory.getInstance());
        add("trim", false, StringMethodsFactory.NTrimMethodNodeFactory.getInstance());
        add("ltrim", false, StringMethodsFactory.NLTrimMethodNodeFactory.getInstance());
        add("rtrim", false, StringMethodsFactory.NRTrimMethodNodeFactory.getInstance());
        add("join", false, StringMethodsFactory.NJoinMethodNodeFactory.getInstance());
        add("index_of", false, StringMethodsFactory.NIndexOfMethodNodeFactory.getInstance());
        add("starts_with", false, StringMethodsFactory.NStartsWithMethodNodeFactory.getInstance());
        add("ends_with", false, StringMethodsFactory.NEndsWithMethodNodeFactory.getInstance());
        add("lpad", false, StringMethodsFactory.NLpadMethodNodeFactory.getInstance());
        add("rpad", false, StringMethodsFactory.NRpadMethodNodeFactory.getInstance());
        add("match", false, StringMethodsFactory.NMatchMethodNodeFactory.getInstance());
      }
    };
  }

  @ImportStatic(ZString.class)
  public abstract static class NKeyDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(TruffleString string, Object item,
                           @Cached TruffleString.CodePointLengthNode lengthNode,
                           @Cached(value = "length(string, lengthNode)", neverDefault = false) long stringLength) {
      if (stringLength == 0) {
        return ZuriNil.SINGLETON;
      } else if (item == ZuriNil.SINGLETON) {
        return 0L;
      }

      if (item instanceof Long index) {
        if (index < stringLength - 1) {
          return index + 1;
        }

        return ZuriNil.SINGLETON;
      }

      return doFallback(string, item);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      throw ZuriRuntimeError.valueError(this, "Strings are numerically indexed");
    }
  }

  @ImportStatic(ZString.class)
  public abstract static class NValueDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(TruffleString string, long index,
                           @Cached TruffleString.CodePointLengthNode lengthNode,
                           @Cached(value = "length(string, lengthNode)", neverDefault = false) long stringLength,
                           @Cached TruffleString.SubstringNode substringNode) {

      if (index > -1 && index < stringLength) {
        return ZString.substring(string, (int) index, 1, substringNode);
      }

      return doFallback(string, index);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      return ZuriNil.SINGLETON;
    }
  }

  public abstract static class NLengthMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(TruffleString string, @Cached TruffleString.CodePointLengthNode lengthNode) {
      return ZString.length(string, lengthNode);
    }
  }

  public abstract static class NIndexOfMethodNode extends NBuiltinFunctionNode {

    @Specialization(guards = "isNil(extra)")
    protected long indexOfNil(
      TruffleString self, TruffleString other, Object extra,
      @Cached @Cached.Shared("indexOfStringNode") TruffleString.IndexOfStringNode indexOfStringNode,
      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode) {
      if (self == ZString.EMPTY) {
        return -1;
      }

      return ZString.indexOf(indexOfStringNode, lengthNode, self, other, 0);
    }

    @Specialization(replaces = "indexOfNil")
    protected long indexOfLong(
      TruffleString self, TruffleString other, long startIndex,
      @Cached @Cached.Shared("indexOfStringNode") TruffleString.IndexOfStringNode indexOfStringNode,
      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode) {
      if (self == ZString.EMPTY) {
        return -1;
      }

      return ZString.indexOf(indexOfStringNode, lengthNode, self, other, (int) startIndex);
    }

    protected boolean isNil(Object o) {
      return o == ZuriNil.SINGLETON;
    }

    @Fallback
    protected Object unknownArguments(Object self, Object other, Object object) {
      throw ZuriRuntimeError.argumentError(this, "string.index_of", other, object);
    }
  }

  public abstract static class NUpperMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doValid(TruffleString self,
                                    @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if (self == ZString.EMPTY) {
        return self;
      }

      return fromJavaStringNode.execute(
        ZString.toUpper(self.toJavaStringUncached()),
        ZuriLanguage.ENCODING);
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw ZuriRuntimeError.argumentError(this, "string.upper");
    }
  }

  public abstract static class NLowerMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doValid(TruffleString self,
                                    @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if (self == ZString.EMPTY) {
        return self;
      }

      return ZString.fromObject(fromJavaStringNode, ZString.toLower(self.toJavaStringUncached()));
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw ZuriRuntimeError.argumentError(this, "string.lower");
    }
  }

  public abstract static class NIsAlphaMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(TruffleString self,
                              @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                              @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if (self == ZString.EMPTY) {
        return false;
      }

      int length = ZString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, ZuriLanguage.ENCODING);
        if (!isAlpha(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw ZuriRuntimeError.argumentError(this, "string.is_alpha");
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isAlpha(int c) {
      return Character.isLetter(c);
    }
  }

  public abstract static class NIsAlNumMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(TruffleString self,
                              @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if (self == ZString.EMPTY) {
        return false;
      }

      int length = ZString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, ZuriLanguage.ENCODING);
        if (!isAlphaNumeric(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw ZuriRuntimeError.argumentError(this, "string.is_alnum");
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isAlphaNumeric(int c) {
      return Character.isLetterOrDigit(c);
    }
  }

  public abstract static class NIsNumberMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(TruffleString self,
                              @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if (self == ZString.EMPTY) {
        return false;
      }

      int length = ZString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, ZuriLanguage.ENCODING);
        if (!isDigit(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw ZuriRuntimeError.argumentError(this, "string.is_number");
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isDigit(int c) {
      return Character.isDigit(c);
    }
  }

  public abstract static class NIsLowerMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(TruffleString self,
                              @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if (self == ZString.EMPTY) {
        return false;
      }

      int length = ZString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, ZuriLanguage.ENCODING);
        if (!isLower(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw ZuriRuntimeError.argumentError(this, "string.is_lower");
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isLower(int c) {
      return Character.isLowerCase(c);
    }
  }

  public abstract static class NIsUpperMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(TruffleString self,
                              @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if (self == ZString.EMPTY) {
        return false;
      }

      int length = ZString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, ZuriLanguage.ENCODING);
        if (!isUpper(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw ZuriRuntimeError.argumentError(this, "string.is_upper");
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isUpper(int c) {
      return Character.isUpperCase(c);
    }
  }

  public abstract static class NIsSpaceMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(TruffleString self,
                              @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if (self == ZString.EMPTY) {
        return false;
      }

      int length = ZString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, ZuriLanguage.ENCODING);
        if (!isSpace(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw ZuriRuntimeError.argumentError(this, "string.is_space");
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isSpace(int c) {
      return Character.isWhitespace(c);
    }
  }

  public abstract static class NStartsWithMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(TruffleString self, TruffleString other,
                              @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.IndexOfStringNode indexOfNode) {
      if (self == ZString.EMPTY) {
        return false;
      }

      return ZString.indexOf(indexOfNode, lengthNode, self, other, 0) == 0;
    }

    @Fallback
    protected Object doInvalid(Object self, Object other) {
      throw ZuriRuntimeError.argumentError(this, "string.starts_with", other);
    }
  }

  public abstract static class NEndsWithMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doValid(TruffleString self, TruffleString other,
                              @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.IndexOfStringNode indexOfNode) {
      if (self == ZString.EMPTY) {
        return false;
      }

      long thisLength = ZString.length(self, lengthNode);
      long otherLength = ZString.length(other, lengthNode);
      long index = ZString.indexOf(indexOfNode, lengthNode, self, other, 0);

      return index > -1 && index + otherLength == thisLength;
    }

    @Fallback
    protected Object doInvalid(Object self, Object other) {
      throw ZuriRuntimeError.argumentError(this, "string.ends_with", other);
    }
  }

  @ImportStatic(ZString.class)
  public abstract static class NTrimMethodNode extends NBuiltinFunctionNode {
    @Specialization(guards = "string == EMPTY")
    protected Object doEmptyString(TruffleString string, Object trimmer) {
      return string;
    }

    @Specialization(guards = "trimmer == EMPTY")
    protected Object doEmptyTrimmer(TruffleString string, TruffleString trimmer) {
      return string;
    }

    @Specialization
    protected Object doDefault(TruffleString string, ZuriNil nil,
                               @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                               @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                               @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      int length = (int) ZString.length(string, lengthNode);
      if (length == 0) {
        return string;
      }

      int start = 0;
      while (start < length && isSpace(charUTF16Node.execute(string, start))) {
        start++;
      }

      int end = length - 1;
      while (end >= start && isSpace(charUTF16Node.execute(string, end))) {
        end--;
      }

      if (start > end) {
        return ZString.EMPTY;
      } else {
        return ZString.substring(string, start, end - start + 1, substringNode);
      }
    }

    @Specialization(guards = "length(item, lengthNode) == 1")
    protected Object doItemSetValid(TruffleString string, TruffleString item,
                                    @Cached @Cached.Shared("equalNode") TruffleString.EqualNode equalNode,
                                    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                    @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                                    @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      char trimmer = charUTF16Node.execute(item, 0);

      int length = (int) ZString.length(string, lengthNode);
      if (length == 0) {
        return string;
      }

      int start = 0;
      while (start < length && charUTF16Node.execute(string, start) == trimmer) {
        start++;
      }

      int end = length - 1;
      while (end >= start && charUTF16Node.execute(string, end) == trimmer) {
        end--;
      }

      if (start > end) {
        return ZString.EMPTY;
      } else {
        return ZString.substring(string, start, end - start + 1, substringNode);
      }
    }

    @Specialization
    protected Object doItemSetInvalid(TruffleString string, TruffleString item,
                                      @Cached @Cached.Shared("equalNode") TruffleString.EqualNode equalNode,
                                      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                      @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                                      @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      throw ZuriRuntimeError.valueError(this, "Char expected in argument 2, string given.");
    }

    @Fallback
    protected Object doInvalid(Object self, Object argument) {
      throw ZuriRuntimeError.argumentError(this, "string.trim()", argument);
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isSpace(int c) {
      return Character.isWhitespace(c);
    }
  }

  @ImportStatic(ZString.class)
  public abstract static class NLTrimMethodNode extends NBuiltinFunctionNode {

    @Specialization(guards = "string == EMPTY")
    protected Object doEmptyString(TruffleString string, Object trimmer) {
      return string;
    }

    @Specialization(guards = "trimmer == EMPTY")
    protected Object doEmptyTrimmer(TruffleString string, TruffleString trimmer) {
      return string;
    }

    @Specialization
    protected Object doDefault(TruffleString string, ZuriNil nil,
                               @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                               @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                               @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      int length = (int) ZString.length(string, lengthNode);
      if (length == 0) {
        return string;
      }

      int start = 0;
      while (start < length && isSpace(charUTF16Node.execute(string, start))) {
        start++;
      }

      if (start == length - 1) {
        return ZString.EMPTY;
      } else {
        return ZString.substring(string, start, length - start, substringNode);
      }
    }

    @Specialization(guards = "length(item, lengthNode) == 1")
    protected Object doItemSetValid(TruffleString string, TruffleString item,
                                    @Cached @Cached.Shared("equalNode") TruffleString.EqualNode equalNode,
                                    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                    @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                                    @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      char trimmer = charUTF16Node.execute(item, 0);

      int length = (int) ZString.length(string, lengthNode);
      if (length == 0) {
        return string;
      }

      int start = 0;
      while (start < length && charUTF16Node.execute(string, start) == trimmer) {
        start++;
      }

      if (start == length - 1) {
        return ZString.EMPTY;
      } else {
        return ZString.substring(string, start, length - start, substringNode);
      }
    }

    @Specialization
    protected Object doItemSetInvalid(TruffleString string, TruffleString item,
                                      @Cached @Cached.Shared("equalNode") TruffleString.EqualNode equalNode,
                                      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                      @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                                      @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      throw ZuriRuntimeError.valueError(this, "Char expected in argument 2, string given.");
    }

    @Fallback
    protected Object doInvalid(Object self, Object argument) {
      throw ZuriRuntimeError.argumentError(this, "string.ltrim()", argument);
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isSpace(int c) {
      return Character.isWhitespace(c);
    }
  }

  @ImportStatic(ZString.class)
  public abstract static class NRTrimMethodNode extends NBuiltinFunctionNode {
    @Specialization(guards = "string == EMPTY")
    protected Object doEmptyString(TruffleString string, Object trimmer) {
      return string;
    }

    @Specialization(guards = "trimmer == EMPTY")
    protected Object doEmptyTrimmer(TruffleString string, TruffleString trimmer) {
      return string;
    }

    @Specialization
    protected Object doDefault(TruffleString string, ZuriNil nil,
                               @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                               @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                               @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      int length = (int) ZString.length(string, lengthNode);
      if (length == 0) {
        return string;
      }

      int end = length - 1;
      while (end > 0 && isSpace(charUTF16Node.execute(string, end))) {
        end--;
      }

      if (end == 0) {
        return ZString.EMPTY;
      } else {
        return ZString.substring(string, 0, end + 1, substringNode);
      }
    }

    @Specialization(guards = "length(item, lengthNode) == 1")
    protected Object doItemSetValid(TruffleString string, TruffleString item,
                                    @Cached @Cached.Shared("equalNode") TruffleString.EqualNode equalNode,
                                    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                    @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                                    @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      char trimmer = charUTF16Node.execute(item, 0);

      int length = (int) ZString.length(string, lengthNode);
      if (length == 0) {
        return string;
      }

      int end = length - 1;
      while (end > 0 && charUTF16Node.execute(string, end) == trimmer) {
        end--;
      }

      if (end == 0) {
        return ZString.EMPTY;
      } else {
        return ZString.substring(string, 0, end + 1, substringNode);
      }
    }

    @Specialization
    protected Object doItemSetInvalid(TruffleString string, TruffleString item,
                                      @Cached @Cached.Shared("equalNode") TruffleString.EqualNode equalNode,
                                      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                      @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                                      @Cached @Cached.Shared("charUTF16Node") TruffleString.ReadCharUTF16Node charUTF16Node) {
      throw ZuriRuntimeError.valueError(this, "Char expected in argument 2, string given.");
    }

    @Fallback
    protected Object doInvalid(Object self, Object argument) {
      throw ZuriRuntimeError.argumentError(this, "string.rtrim()", argument);
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isSpace(int c) {
      return Character.isWhitespace(c);
    }
  }

  public abstract static class NJoinMethodNode extends NBuiltinFunctionNode {

    @Specialization(guards = "list.getArraySize() == 0")
    public static Object doEmptyList(TruffleString string, ListObject list) {
      return list;
    }

    @Specialization(guards = "list.getArraySize() > 0")
    public static Object doList(TruffleString string, ListObject list, @Bind Node node,
                                @Cached @Cached.Shared("toStringNode") NToStringNode toStringNode,
                                @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
      Object[] items = list.getItems();
      final long length = list.getArraySize();

      TruffleString result = toStringNode.execute(node, items[0]);
      for (int i = 1; i < length; i++) {
        result = ZString.concat(concatNode, result, string);
        result = ZString.concat(concatNode, result, toStringNode.execute(node, items[i]));
      }

      return result;
    }

    @Specialization(guards = {"!string.isEmpty()", "!item.isEmpty()"})
    public static Object doString(TruffleString string, TruffleString item, @Bind Node node,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                                  @Cached TruffleString.ReadCharUTF16Node readCharUTF16Node,
                                  @Cached @Cached.Shared("toStringNode") NToStringNode toStringNode,
                                  @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
      final long length = ZString.length(item, lengthNode);

      TruffleString result = toStringNode.execute(node, readCharUTF16Node.execute(item, 0));
      for (int i = 1; i < length; i++) {
        result = ZString.concat(concatNode, result, string);
        result = ZString.concat(concatNode, result, toStringNode.execute(node, readCharUTF16Node.execute(item, i)));
      }

      return result;
    }

    @Specialization(limit = "3")
    public static Object doDictionary(TruffleString string, DictionaryObject dictionary,
                                      @Bind Node node,
                                      @Cached @Cached.Shared("toStringNode") NToStringNode toStringNode,
                                      @CachedLibrary("dictionary") DynamicObjectLibrary objectLibrary,
                                      @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
      Object[] keys = objectLibrary.getKeyArray(dictionary);
      final int length = keys.length;
      if (length == 0) {
        return dictionary;
      }

      TruffleString result = toStringNode.execute(node, keys[0]);
      for (int i = 1; i < length; i++) {
        result = ZString.concat(concatNode, result, string);
        result = ZString.concat(concatNode, result, toStringNode.execute(node, keys[i]));
      }

      return result;
    }

    @Specialization(guards = "string.isEmpty()")
    public static Object doEmptyString(TruffleString string, Object items) {
      return items;
    }

    @Specialization(guards = "items.isEmpty()")
    public static Object doEmptyString(TruffleString string, TruffleString items) {
      return ZString.EMPTY;
    }

    @Fallback
    protected static Object doFallback(Object object, Object iterable, @Bind Node node) {
      throw ZuriRuntimeError.argumentError(node, "string.join()", iterable);
    }
  }

  @ImportStatic(ZString.class)
  public abstract static class NLpadMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doLpad(TruffleString self, long width, ZuriNil nil,
                                   @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                   @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode,
                                   @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode) {
      long length = ZString.length(self, lengthNode);
      if (width <= length || width < 0) {
        return self;
      }
      long padCount = width - length;
      TruffleString padString = createPadString(padCount, ' ', fromJavaStringNode);
      return ZString.concat(concatNode, padString, self);
    }

    @Specialization(guards = "length(fill, lengthNode) == 1")
    protected TruffleString doLpadWithFill(TruffleString self, long width, TruffleString fill,
                                           @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                           @Cached TruffleString.ReadCharUTF16Node readCharNode,
                                           @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode,
                                           @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode) {
      long length = ZString.length(self, lengthNode);
      if (width <= length || width < 0) {
        return self;
      }
      long padCount = width - length;
      char fillChar = (char) readCharNode.execute(fill, 0);
      TruffleString padString = createPadString(padCount, fillChar, fromJavaStringNode);
      return ZString.concat(concatNode, padString, self);
    }

    @Fallback
    protected Object doInvalid(Object self, Object width, Object fill) {
      throw ZuriRuntimeError.argumentError(this, "string.lpad", width, fill);
    }

    @CompilerDirectives.TruffleBoundary
    private TruffleString createPadString(long count, char fillChar,
                                          TruffleString.FromJavaStringNode fromJavaStringNode) {
      StringBuilder sb = new StringBuilder((int) count);
      for (long i = 0; i < count; i++) {
        sb.append(fillChar);
      }
      return fromJavaStringNode.execute(sb.toString(), ZuriLanguage.ENCODING);
    }
  }

  @ImportStatic(ZString.class)
  public abstract static class NRpadMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doRpad(TruffleString self, long width, ZuriNil nil,
                                   @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                   @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode,
                                   @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode) {
      long length = ZString.length(self, lengthNode);
      if (width <= length || width < 0) {
        return self;
      }
      long padCount = width - length;
      TruffleString padString = createPadString(padCount, ' ', fromJavaStringNode);
      return ZString.concat(concatNode, self, padString);
    }

    @Specialization(guards = "length(fill, lengthNode) == 1")
    protected TruffleString doRpadWithFill(TruffleString self, long width, TruffleString fill,
                                           @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                                           @Cached TruffleString.ReadCharUTF16Node readCharNode,
                                           @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode,
                                           @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode) {
      long length = ZString.length(self, lengthNode);
      if (width <= length || width < 0) {
        return self;
      }
      long padCount = width - length;
      char fillChar = readCharNode.execute(fill, 0);
      TruffleString padString = createPadString(padCount, fillChar, fromJavaStringNode);
      return ZString.concat(concatNode, self, padString);
    }

    @Fallback
    protected Object doInvalid(Object self, Object width, Object fill) {
      throw ZuriRuntimeError.argumentError(this, "string.rpad", width, fill);
    }

    @CompilerDirectives.TruffleBoundary
    private TruffleString createPadString(long count, char fillChar,
                                          TruffleString.FromJavaStringNode fromJavaStringNode) {
      StringBuilder sb = new StringBuilder((int) count);
      for (long i = 0; i < count; i++) {
        sb.append(fillChar);
      }
      return fromJavaStringNode.execute(sb.toString(), ZuriLanguage.ENCODING);
    }
  }

  public abstract static class NMatchMethodNode extends NBuiltinFunctionNode {
    @Specialization(guards = "isNil(startIndex)")
    protected Object doMatchFromZero(
      TruffleString input,
      TruffleString pattern,
      Object startIndex,
      @Cached @Cached.Shared("stringLengthNode") TruffleString.CodePointLengthNode stringLengthNode,
      @Cached @Cached.Shared("toJavaString") TruffleString.ToJavaStringNode toJavaString,
      @CachedLibrary(limit = "3") @Cached.Shared("regexInterop") InteropLibrary regexInterop,
      @CachedLibrary(limit = "3") @Cached.Shared("resultInterop") InteropLibrary resultInterop,
      @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
      @Cached(value = "classesModel.dictionaryShape", neverDefault = true) Shape shape,
      @Cached(value = "classesModel.dictionaryObject", neverDefault = true) ZuriClass classObject
    ) {
      return doMatch(input, pattern, 0, stringLengthNode, toJavaString, regexInterop, resultInterop, shape, classObject);
    }

    @Specialization(replaces = "doMatchFromZero")
    protected Object doMatchFromIndex(
      TruffleString input,
      TruffleString pattern,
      long startIndex,
      @Cached @Cached.Shared("stringLengthNode") TruffleString.CodePointLengthNode stringLengthNode,
      @Cached @Cached.Shared("toJavaString") TruffleString.ToJavaStringNode toJavaString,
      @CachedLibrary(limit = "3") @Cached.Shared("regexInterop") InteropLibrary regexInterop,
      @CachedLibrary(limit = "3") @Cached.Shared("resultInterop") InteropLibrary resultInterop,
      @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
      @Cached(value = "classesModel.dictionaryShape", neverDefault = true) Shape shape,
      @Cached(value = "classesModel.dictionaryObject", neverDefault = true) ZuriClass classObject
    ) {
      return doMatch(input, pattern, startIndex, stringLengthNode, toJavaString, regexInterop, resultInterop, shape, classObject);
    }

    @Fallback
    protected Object doFallback(Object object, Object other, Object startIndex) {
      throw ZuriRuntimeError.argumentError(this, "string.match()", other, startIndex);
    }

    protected boolean isNil(Object o) {
      return o == ZuriNil.SINGLETON;
    }

    protected Object doMatch(
      TruffleString input, TruffleString pattern, long startIndex,
      TruffleString.CodePointLengthNode stringLengthNode, TruffleString.ToJavaStringNode toJavaString,
      InteropLibrary regexInterop, InteropLibrary resultInterop, Shape shape, ZuriClass classObject
    ) {
      int inputLength = ZString.intLength(input, stringLengthNode);
      int patternLength = ZString.intLength(pattern, stringLengthNode);

      if (patternLength == 0 || inputLength == 0) {
        return false;
      }

      String patternStr = toJavaString.execute(pattern);
      String inputStr = toJavaString.execute(input);

      RegexObject compiled = ZuriContext.get(this).getRegexCache().compile(patternStr, regexInterop);

      RegexResult result = (RegexResult) execMatch(this, regexInterop, compiled, inputStr, (int) startIndex);

      return buildGroupsDictionary(this, resultInterop, compiled, result, inputStr, shape, classObject);
    }

    private static Object execMatch(Node node, InteropLibrary interop, RegexObject compiled,
                                    String input, int fromIndex) {
      try {
        return interop.invokeMember(compiled, "exec", input, fromIndex);
      } catch (Exception e) {
        throw ZuriRuntimeError.error(node, e.getMessage());
      }
    }

    @CompilerDirectives.TruffleBoundary
    private static Object buildGroupsDictionary(
      Node node, InteropLibrary interop,
      RegexObject regex,
      RegexResult result,
      String input, Shape shape, ZuriClass classObject
    ) {
      try {
        boolean isMatch = (boolean) interop.readMember(result, "isMatch");
        if (!isMatch) return false;

        Object getStart = interop.readMember(result, "getStart");
        Object getEnd = interop.readMember(result, "getEnd");

        List<Object> groupIds = new ArrayList<>();
        List<Object> groupValues = new ArrayList<>();

        // Add numbered groups
        for (int i = 0; i < regex.getNumberOfCaptureGroups(); i++) {
          int start = (int) interop.execute(getStart, i);
          int end = (int) interop.execute(getEnd, i);

          // -1 on both means group index is out of bounds — we're done
          if (start == -1 && end == -1 && i > 0)
            break;

          groupIds.add(i);
          groupValues.add(start == -1
            ? ZuriNil.SINGLETON          // unmatched optional group
            : input.substring(start, end));
        }

        // Add named groups
        Object groupsObj = interop.readMember(regex, "groups");
        if (!interop.isNull(groupsObj)) {
          Object groupNames = interop.getMembers(groupsObj);
          long nameCount = interop.getArraySize(groupNames);

          for (long n = 0; n < nameCount; n++) {
            String name = (String) interop.readArrayElement(groupNames, n);

            int start = (int) interop.execute(getStart, n);
            int end = (int) interop.execute(getEnd, n);

            groupIds.add(ZString.fromJavaString(name));
            groupValues.add(start == -1
              ? ZuriNil.SINGLETON          // unmatched optional group
              : input.substring(start, end));
          }
        }

        return new DictionaryObject(shape, classObject, groupIds.toArray(), groupValues.toArray());
      } catch (Exception e) {
        throw ZuriRuntimeError.error(node, e.getMessage());
      }
    }
  }
}
