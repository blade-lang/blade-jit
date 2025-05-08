package org.blade.language.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
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
      add("index_of", false, StringMethodsFactory.NStringIndexOfMethodNodeFactory.getInstance());
      add("upper", false, StringMethodsFactory.NStringUpperMethodNodeFactory.getInstance());
      add("lower", false, StringMethodsFactory.NStringLowerMethodNodeFactory.getInstance());
    }};
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
