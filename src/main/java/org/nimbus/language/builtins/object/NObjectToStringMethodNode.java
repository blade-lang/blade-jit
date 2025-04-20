package org.nimbus.language.builtins.object;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.runtime.NString;

public abstract class NObjectToStringMethodNode extends NBuiltinFunctionNode {
  @Specialization
  protected TruffleString doObject(DynamicObject self,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
    return NString.fromObject(fromJavaStringNode, self);
  }

  @Fallback
  protected Object doPrimitive(Object self) {
    return NString.fromObject(self);
  }
}
