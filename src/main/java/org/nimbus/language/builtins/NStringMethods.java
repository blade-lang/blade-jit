package org.nimbus.language.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NBaseBuiltinDeclaration;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.runtime.NString;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;
import org.nimbus.utility.RegulatedMap;

public class NStringMethods implements NBaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("index_of", false, NStringMethodsFactory.NStringIndexOfMethodNodeFactory.getInstance());
      add("upper", false, NStringMethodsFactory.NStringUpperMethodNodeFactory.getInstance());
      add("lower", false, NStringMethodsFactory.NStringLowerMethodNodeFactory.getInstance());
    }};
  }

  public abstract static class NStringIndexOfMethodNode extends NBuiltinFunctionNode {

    @Specialization(guards = "isNil(extra)")
    protected int indexOfNil(
      TruffleString self, TruffleString other, Object extra,
      @Cached @Cached.Shared("indexOfStringNode") TruffleString.IndexOfStringNode indexOfStringNode,
      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode
    ) {
      if(self == NString.EMPTY) {
        return -1;
      }

      return NString.indexOf(indexOfStringNode, lengthNode, self, other, 0);
    }

    @Specialization(replaces = "indexOfNil")
    protected int indexOfInt(
      TruffleString self, TruffleString other, int startIndex,
      @Cached @Cached.Shared("indexOfStringNode") TruffleString.IndexOfStringNode indexOfStringNode,
      @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode
    ) {
      if(self == NString.EMPTY) {
        return -1;
      }

      return NString.indexOf(indexOfStringNode, lengthNode, self, other, startIndex);
    }

    protected boolean isNil(Object o) {
      return o == NimNil.SINGLETON;
    }

    @Fallback
    protected Object unknownArguments(Object self, Object other, Object object) {
      throw NimRuntimeError.argumentError(this, "string.index_of", other, object);
    }
  }

  public abstract static class NStringUpperMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString upper(TruffleString self,
                                  @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if(self == NString.EMPTY) {
        return self;
      }

      return fromJavaStringNode.execute(
        NString.toUpper(self.toJavaStringUncached()),
        NimbusLanguage.ENCODING
      );
    }

    @Fallback
    protected Object upper(Object self) {
      throw NimRuntimeError.create("invalid call to string.upper()");
    }
  }

  public abstract static class NStringLowerMethodNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString upper(TruffleString self,
                                  @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if(self == NString.EMPTY) {
        return self;
      }

      return fromJavaStringNode.execute(
        NString.toLower(self.toJavaStringUncached()),
        NimbusLanguage.ENCODING
      );
    }

    @Fallback
    protected Object upper(Object self) {
      throw NimRuntimeError.create("invalid call to string.lower()");
    }
  }
}
