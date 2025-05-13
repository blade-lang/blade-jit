package org.blade.language.builtins;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BaseBuiltinDeclaration;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BladeNil;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.ListObject;
import org.blade.utility.RegulatedMap;

public class StringMethods implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("@key", false, StringMethodsFactory.NKeyDecoratorNodeFactory.getInstance());
      add("@value", false, StringMethodsFactory.NValueDecoratorNodeFactory.getInstance());
      add("index_of", false, StringMethodsFactory.NStringIndexOfMethodNodeFactory.getInstance());
      add("upper", false, StringMethodsFactory.NStringUpperMethodNodeFactory.getInstance());
      add("lower", false, StringMethodsFactory.NStringLowerMethodNodeFactory.getInstance());
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

  public abstract static class NStringIndexOfMethodNode extends NBuiltinFunctionNode {

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
      throw BladeRuntimeError.argumentError(this, "string.index_of", other, object);
    }
  }

  public abstract static class NStringUpperMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString upper(TruffleString self,
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
    protected Object upper(Object self) {
      throw BladeRuntimeError.create("invalid call to string.upper()");
    }
  }

  public abstract static class NStringLowerMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString upper(TruffleString self,
                                  @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if(self == BString.EMPTY) {
        return self;
      }

      return fromJavaStringNode.execute(
        BString.toLower(self.toJavaStringUncached()),
        BladeLanguage.ENCODING
      );
    }

    @Fallback
    protected Object upper(Object self) {
      throw BladeRuntimeError.create("invalid call to string.lower()");
    }
  }
}
