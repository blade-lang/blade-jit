package org.nimbus.language.builtins.string;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.runtime.NString;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NStringUpperMethodNode extends NBuiltinFunctionNode {
//  @CompilerDirectives.TruffleBoundary
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
    throw new NimRuntimeError("invalid call to string.upper()");
  }
}
