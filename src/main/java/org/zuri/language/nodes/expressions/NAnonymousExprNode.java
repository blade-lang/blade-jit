package org.zuri.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.zuri.language.nodes.NNode;
import org.zuri.language.runtime.ZuriContext;
import org.zuri.language.runtime.ZuriRuntimeError;

public final class NAnonymousExprNode extends NNode {
  static final DynamicObjectLibrary objectLibrary = DynamicObjectLibrary.getUncached();
  @SuppressWarnings("FieldMayBeFinal")
  @Executed
  @Child
  private NNode function;

  public NAnonymousExprNode(NNode function) {
    this.function = function;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    function.execute(frame);

    Object value = objectLibrary.getOrDefault(ZuriContext.get(this).globalScope, "@anonymous", null);
    if (value == null) {
      throw ZuriRuntimeError.error(this, "Failed to create anonymous function.");
    }
    return value;
  }
}
