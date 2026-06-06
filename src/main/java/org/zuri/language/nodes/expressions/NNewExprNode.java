package org.zuri.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.functions.NMethodDispatchNode;
import org.zuri.language.runtime.*;

import java.util.List;

@ImportStatic(ZuriContext.class)
public abstract class NNewExprNode extends NNode {
  @Children
  private final NNode[] arguments;
  @Child
  @Executed
  protected NNode constructor;

  public NNewExprNode(NNode constructor, List<NNode> arguments) {
    this.constructor = constructor;
    this.arguments = arguments.toArray(new NNode[0]);
  }

  @SuppressWarnings({"truffle-neverdefault", "truffle-static-method"})
  @Specialization(limit = "3")
  protected Object doObject(VirtualFrame frame, ZuriClass classObject,
                            @Bind Node node,
                            @Cached InlinedCountingConditionProfile isMethodProfile,
                            @Cached("get(node)") ZuriContext context,
                            @Cached NMethodDispatchNode methodDispatchNode,
                            @Cached(value = "context.objectsModel.rootShape") Shape rootShape,
                            @CachedLibrary("classObject") InteropLibrary interopLibrary) {
    ZuriObject object = new ZuriObject(rootShape, classObject);
    Object constructor = null;
    try {
      constructor = interopLibrary.readMember(classObject, "@new");
    } catch (UnsupportedMessageException e) {
      throw ZuriRuntimeError.error(node, e.getMessage());
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
    throw ZuriRuntimeError.error(this, "'", object, "' is not a constructor");
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
