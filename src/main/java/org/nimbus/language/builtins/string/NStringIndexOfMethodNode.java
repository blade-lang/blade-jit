package org.nimbus.language.builtins.string;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.NBuiltinFunctionNode;
import org.nimbus.language.runtime.NString;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NStringIndexOfMethodNode extends NBuiltinFunctionNode {

  @Specialization(guards = "isNil(extra)")
  protected long indexOfNil(
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
  protected long indexOfLong(
    TruffleString self, TruffleString other, long startIndex,
    @Cached @Cached.Shared("indexOfStringNode") TruffleString.IndexOfStringNode indexOfStringNode,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode
  ) {
    if(self == NString.EMPTY) {
      return -1;
    }

    return NString.indexOf(indexOfStringNode, lengthNode, self, other, (int)startIndex);
  }

  protected boolean isNil(Object o) {
    return o == NimNil.SINGLETON;
  }

  @Fallback
  protected Object unknownArguments(Object self, Object other, Object object) {
    throw NimRuntimeError.argumentError(this, "string.index_of", other, object);
  }
}
