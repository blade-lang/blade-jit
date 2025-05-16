package org.blade.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BigIntObject;

import java.math.BigInteger;

@NodeChild(value = "arguments", type = NReadFunctionArgsExprNode[].class)
@GenerateNodeFactory
public abstract class NBuiltinFunctionNode extends NNode {
  protected boolean isDouble(Object object) {
    return object instanceof Double;
  }

  protected boolean isLong(Object object) {
    return object instanceof Long;
  }

  protected boolean isString(Object object) {
    return object instanceof TruffleString;
  }

  @CompilerDirectives.TruffleBoundary
  protected boolean objectEquals(Object first, Object second) {
    return first.equals(second);
  }
}
