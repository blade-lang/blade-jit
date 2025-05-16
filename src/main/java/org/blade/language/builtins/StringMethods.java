package org.blade.language.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BaseBuiltinDeclaration;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BladeNil;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.utility.RegulatedMap;

public class StringMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("@key", false, StringMethodsFactory.NKeyDecoratorNodeFactory.getInstance());
      add("@value", false, StringMethodsFactory.NValueDecoratorNodeFactory.getInstance());
      add("index_of", false, StringMethodsFactory.NIndexOfMethodNodeFactory.getInstance());
      add("upper", false, StringMethodsFactory.NUpperMethodNodeFactory.getInstance());
      add("lower", false, StringMethodsFactory.NLowerMethodNodeFactory.getInstance());
      add("is_alpha", false, StringMethodsFactory.NIsAlphaMethodNodeFactory.getInstance());
      add("is_alnum", false, StringMethodsFactory.NIsAlNumMethodNodeFactory.getInstance());
      add("is_number", false, StringMethodsFactory.NIsNumberMethodNodeFactory.getInstance());
      add("is_lower", false, StringMethodsFactory.NIsLowerMethodNodeFactory.getInstance());
      add("is_upper", false, StringMethodsFactory.NIsUpperMethodNodeFactory.getInstance());
      add("is_space", false, StringMethodsFactory.NIsSpaceMethodNodeFactory.getInstance());
      add("starts_with", false, StringMethodsFactory.NStartsWithMethodNodeFactory.getInstance());
      add("ends_with", false, StringMethodsFactory.NEndsWithMethodNodeFactory.getInstance());
    }};
  }


  @ImportStatic(BString.class)
  public abstract static class NKeyDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(TruffleString string, Object item,
                           @Cached TruffleString.CodePointLengthNode lengthNode,
                           @Cached(value = "length(string, lengthNode)", neverDefault = false) long stringLength) {
      if(stringLength == 0) {
        return BladeNil.SINGLETON;
      } else if(item == BladeNil.SINGLETON) {
        return 0L;
      }

      if(item instanceof Long index) {
        if(index < stringLength - 1) {
          return index + 1;
        }

        return BladeNil.SINGLETON;
      }

      return doFallback(string, item);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      throw BladeRuntimeError.valueError(this, "Strings are numerically indexed");
    }
  }

  @ImportStatic(BString.class)
  public abstract static class NValueDecoratorNode extends NBuiltinFunctionNode {
    @Specialization
    protected Object doAny(TruffleString string, long index,
                           @Cached TruffleString.CodePointLengthNode lengthNode,
                           @Cached(value = "length(string, lengthNode)", neverDefault = false) long stringLength,
                           @Cached TruffleString.SubstringNode substringNode) {

      if(index > -1 && index < stringLength) {
        return BString.substring(string, (int)index, 1, substringNode);
      }

      return doFallback(string, index);
    }

    @Fallback
    protected Object doFallback(Object object, Object index) {
      return BladeNil.SINGLETON;
    }
  }

  public abstract static class NIndexOfMethodNode extends NBuiltinFunctionNode {

    @Specialization(guards = "isNil(extra)")
    protected long indexOfNil(
      TruffleString self, TruffleString other, Object extra,
      @Cached @Cached.Shared("indexOfStringNode") TruffleString.IndexOfStringNode indexOfStringNode,
      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode
    ) {
      if(self == BString.EMPTY) {
        return -1;
      }

      return BString.indexOf(indexOfStringNode, lengthNode, self, other, 0);
    }

    @Specialization(replaces = "indexOfNil")
    protected long indexOfLong(
      TruffleString self, TruffleString other, long startIndex,
      @Cached @Cached.Shared("indexOfStringNode") TruffleString.IndexOfStringNode indexOfStringNode,
      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode
    ) {
      if(self == BString.EMPTY) {
        return -1;
      }

      return BString.indexOf(indexOfStringNode, lengthNode, self, other, (int)startIndex);
    }

    protected boolean isNil(Object o) {
      return o == BladeNil.SINGLETON;
    }

    @Fallback
    protected Object unknownArguments(Object self, Object other, Object object) {
      throw BladeRuntimeError.argumentError(this, "string.index_of", self, other, object);
    }
  }

  public abstract static class NUpperMethodNode extends NBuiltinFunctionNode {
    @ExplodeLoop
    @Specialization
    protected TruffleString doValid(TruffleString self,
                                  @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if(self == BString.EMPTY) {
        return self;
      }

      return fromJavaStringNode.execute(
        BString.toUpper(self.toJavaStringUncached()),
        BladeLanguage.ENCODING
      );
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw BladeRuntimeError.argumentError(this, "string.upper", self);
    }
  }

  public abstract static class NLowerMethodNode extends NBuiltinFunctionNode {
    @ExplodeLoop
    @Specialization
    protected TruffleString doValid(TruffleString self,
                                  @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if(self == BString.EMPTY) {
        return self;
      }

      return BString.fromObject(fromJavaStringNode, BString.toLower(self.toJavaStringUncached()));
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw BladeRuntimeError.argumentError(this, "string.lower", self);
    }
  }

  public abstract static class NIsAlphaMethodNode extends NBuiltinFunctionNode {
    @ExplodeLoop
    @Specialization
    protected boolean doValid(TruffleString self,
                                  @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if(self == BString.EMPTY) {
        return false;
      }

      int length = BString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, BladeLanguage.ENCODING);
        if (!isAlpha(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw BladeRuntimeError.argumentError(this, "string.is_alpha", self);
    }

    @ExplodeLoop
    @CompilerDirectives.TruffleBoundary
    private boolean isAlpha(int c) {
      return Character.isLetter(c);
    }
  }

  public abstract static class NIsAlNumMethodNode extends NBuiltinFunctionNode {
    @ExplodeLoop
    @Specialization
    protected boolean doValid(TruffleString self,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if(self == BString.EMPTY) {
        return false;
      }

      int length = BString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, BladeLanguage.ENCODING);
        if (!isAlphaNumeric(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw BladeRuntimeError.argumentError(this, "string.is_alnum", self);
    }

    @ExplodeLoop
    @CompilerDirectives.TruffleBoundary
    private boolean isAlphaNumeric(int c) {
      return Character.isLetterOrDigit(c);
    }
  }

  public abstract static class NIsNumberMethodNode extends NBuiltinFunctionNode {
    @ExplodeLoop
    @Specialization
    protected boolean doValid(TruffleString self,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if(self == BString.EMPTY) {
        return false;
      }

      int length = BString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, BladeLanguage.ENCODING);
        if (!isDigit(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw BladeRuntimeError.argumentError(this, "string.is_number", self);
    }

    @ExplodeLoop
    @CompilerDirectives.TruffleBoundary
    private boolean isDigit(int c) {
      return Character.isDigit(c);
    }
  }

  public abstract static class NIsLowerMethodNode extends NBuiltinFunctionNode {
    @ExplodeLoop
    @Specialization
    protected boolean doValid(TruffleString self,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if(self == BString.EMPTY) {
        return false;
      }

      int length = BString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, BladeLanguage.ENCODING);
        if (!isLower(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw BladeRuntimeError.argumentError(this, "string.is_lower", self);
    }

    @ExplodeLoop
    @CompilerDirectives.TruffleBoundary
    private boolean isLower(int c) {
      return Character.isLowerCase(c);
    }
  }

  public abstract static class NIsUpperMethodNode extends NBuiltinFunctionNode {
    @ExplodeLoop
    @Specialization
    protected boolean doValid(TruffleString self,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                              @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if(self == BString.EMPTY) {
        return false;
      }

      int length = BString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, BladeLanguage.ENCODING);
        if (!isUpper(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw BladeRuntimeError.argumentError(this, "string.is_upper", self);
    }

    @ExplodeLoop
    @CompilerDirectives.TruffleBoundary
    private boolean isUpper(int c) {
      return Character.isUpperCase(c);
    }
  }

  public abstract static class NIsSpaceMethodNode extends NBuiltinFunctionNode {

    @ExplodeLoop
    @Specialization
    protected boolean doValid(TruffleString self,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                                @Cached TruffleString.CodePointAtIndexNode codePointNode) {
      if(self == BString.EMPTY) {
        return false;
      }

      int length = BString.intLength(self, lengthNode);
      for (int i = 0; i < length; i++) {
        int c = codePointNode.execute(self, i, BladeLanguage.ENCODING);
        if (!isSpace(c)) {
          return false;
        }
      }

      return true;
    }

    @Fallback
    protected Object doInvalid(Object self) {
      throw BladeRuntimeError.argumentError(this, "string.is_space", self);
    }

    @CompilerDirectives.TruffleBoundary
    private boolean isSpace(int c) {
      return Character.isSpaceChar(c);
    }
  }

  public abstract static class NStartsWithMethodNode extends NBuiltinFunctionNode {

    @ExplodeLoop
    @Specialization
    protected boolean doValid(TruffleString self, TruffleString other,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                                @Cached TruffleString.IndexOfStringNode indexOfNode) {
      if(self == BString.EMPTY) {
        return false;
      }

      return BString.indexOf(indexOfNode, lengthNode, self, other, 0) == 0;
    }

    @Fallback
    protected Object doInvalid(Object self, Object other) {
      throw BladeRuntimeError.argumentError(this, "string.starts_with", self, other);
    }
  }

  public abstract static class NEndsWithMethodNode extends NBuiltinFunctionNode {

    @ExplodeLoop
    @Specialization
    protected boolean doValid(TruffleString self, TruffleString other,
                                  @Cached TruffleString.CodePointLengthNode lengthNode,
                                @Cached TruffleString.IndexOfStringNode indexOfNode) {
      if(self == BString.EMPTY) {
        return false;
      }

      long thisLength = BString.length(self, lengthNode);
      long otherLength = BString.length(other, lengthNode);
      long index = BString.indexOf(indexOfNode, lengthNode, self, other, 0);

      return index > -1 && index + otherLength == thisLength;
    }

    @Fallback
    protected Object doInvalid(Object self, Object other) {
      throw BladeRuntimeError.argumentError(this, "string.ends_with", self, other);
    }
  }
}
