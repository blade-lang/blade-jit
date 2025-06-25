package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.*;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.functions.NMethodDispatchNode;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;
import org.blade.language.runtime.*;

import java.util.List;

@ImportStatic(BladeContext.class)
public abstract class NNewExprNode extends NNode {
  @Child
  @Executed
  protected NNode constructor;

  @Children
  private final NNode[] arguments;

  public NNewExprNode(NNode constructor, List<NNode> arguments) {
    this.constructor = constructor;
    this.arguments = arguments.toArray(new NNode[0]);
  }

  @SuppressWarnings({"truffle-neverdefault", "truffle-static-method"})
  @Specialization(limit = "3")
  protected Object doObject(VirtualFrame frame, BladeClass classObject,
                            @Bind Node node,
                            @Cached InlinedCountingConditionProfile isMethodProfile,
                            @Cached("get(node)") BladeContext context,
                            @Cached NMethodDispatchNode methodDispatchNode,
                            @Cached(value = "context.objectsModel.rootShape") Shape rootShape,
                            @CachedLibrary("classObject") InteropLibrary interopLibrary) {
    BladeObject object = new BladeObject(rootShape, classObject);
    Object constructor = null;
    try {
      constructor = interopLibrary.readMember(classObject, "@new");
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.error(node, e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if (isMethodProfile.profile(node, constructor instanceof FunctionObject)) {
      methodDispatchNode.executeDispatch(constructor, object, executeArguments(frame));
    } else {
      consumeArguments(frame);
    }
    return object;
  }

  @Fallback
  protected Object doNonConstructor(VirtualFrame frame, Object object) {
    consumeArguments(frame);
    throw BladeRuntimeError.error(this, "'", object, "' is not a constructor");
  }

  @ExplodeLoop
  private void consumeArguments(VirtualFrame frame) {
    for (NNode argument : arguments) {
      argument.execute(frame);
    }
  }

  @ExplodeLoop
  private Object[] executeArguments(VirtualFrame frame) {
    int argumentLength = arguments.length;
    Object[] args = new Object[argumentLength];
    for (int i = 0; i < argumentLength; i++) {
      args[i] = arguments[i].execute(frame);
    }
    return args;
  }
}
