package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.functions.NMethodDispatchNode;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;
import org.blade.language.runtime.*;

import java.util.List;

public abstract class NNewExprNode extends NNode {
  @Child
  @Executed
  protected NNode constructor;

  @Children private final NNode[] arguments;

  @Child
  @SuppressWarnings("FieldMayBeFinal")
  private NMethodDispatchNode constructorDispatch;

  public NNewExprNode(NNode constructor, List<NNode> arguments) {
    this.constructor = constructor;
    this.arguments = arguments.toArray(new NNode[0]);
    constructorDispatch = NMethodDispatchNodeGen.create();
  }

  @Specialization(limit = "3")
  protected Object doObject(VirtualFrame frame, BladeClass classObject,
                            @CachedLibrary("classObject") InteropLibrary interopLibrary) {
    BladeObject object = new BladeObject(languageContext().objectsModel.rootShape, classObject);
    Object constructor = null;
    try {
      constructor = interopLibrary.readMember(classObject, "@new");
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.error(this, e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if(constructor instanceof FunctionObject constructorFunction) {
      constructorDispatch.executeDispatch(constructorFunction, object, executeArguments(frame));
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
    Object[] args = new Object[arguments.length];
    for(int i = 0; i < arguments.length; i++) {
      args[i] = arguments[i].execute(frame);
    }
    return args;
  }
}
