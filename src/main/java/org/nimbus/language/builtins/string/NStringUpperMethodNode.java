package org.nimbus.language.builtins.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NBuiltinFunctionNode;
import org.nimbus.language.runtime.NString;

public abstract class NStringUpperMethodNode extends NBuiltinFunctionNode {
//  @CompilerDirectives.TruffleBoundary
  @Specialization
  protected TruffleString upper(TruffleString self,
                                @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
    if(self == NString.EMPTY) {
      return self;
    }

    return fromJavaStringNode.execute(
      self.toJavaStringUncached().toUpperCase(),
      NimbusLanguage.ENCODING
    );
  }
}
