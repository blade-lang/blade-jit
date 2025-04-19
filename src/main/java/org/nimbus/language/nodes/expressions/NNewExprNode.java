package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimObject;
import org.nimbus.language.runtime.NimClass;
import org.nimbus.language.runtime.NimRuntimeError;

import java.util.List;

public abstract class NNewExprNode extends NNode {
  @Child
  @Executed
  protected NNode constructor;

  @Children private final NNode[] arguments;

  public NNewExprNode(NNode constructor, List<NNode> arguments) {
    this.constructor = constructor;
    this.arguments = arguments.toArray(new NNode[0]);
  }

  @Specialization
  protected Object doObject(VirtualFrame frame, NimClass classObject) {
    consumeArguments(frame);
    return new NimObject(languageContext().objectsModel.rootShape, classObject);
  }

  @Fallback
  protected Object doNonConstructor(VirtualFrame frame, Object object) {
    consumeArguments(frame);
    throw new NimRuntimeError("'" + object + "' is not a constructor");
  }

  @ExplodeLoop
  private void consumeArguments(VirtualFrame frame) {
    for (NNode argument : arguments) {
      argument.execute(frame);
    }
  }
}
