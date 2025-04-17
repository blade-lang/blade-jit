package org.nimbus.language.builtins.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NBuiltinFunctionNode;
import org.nimbus.language.nodes.calls.NReadFunctionArgsExprNode;
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
    if(other instanceof TruffleString) {
      throw new NimRuntimeError("string.index_of() expects number in argument 2");
    }
    throw new NimRuntimeError("string.index_of() expects string in argument 1");
  }
}
