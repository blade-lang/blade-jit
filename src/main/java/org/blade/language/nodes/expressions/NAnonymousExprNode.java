package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeContext;
import org.blade.language.runtime.BladeRuntimeError;

public final class NAnonymousExprNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Executed
  @Child
  private NNode function;

  static final DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();

  public NAnonymousExprNode(NNode function) {
    this.function = function;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    function.execute(frame);

    Object value = objectLibrary.getOrDefault(BladeContext.get(this).globalScope, "@anonymous", null);
    if (value == null) {
      throw BladeRuntimeError.error(this, "Failed to create anonymous function.");
    }
    return value;
  }
}
