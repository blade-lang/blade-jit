package org.nimbus.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.functions.NMethodDispatchNode;
import org.nimbus.language.nodes.functions.NMethodDispatchNodeGen;
import org.nimbus.language.runtime.*;

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
  protected Object doObject(VirtualFrame frame, NimClass classObject,
                            @CachedLibrary("classObject") DynamicObjectLibrary objectLibrary) {
    NimObject object = new NimObject(languageContext().objectsModel.rootShape, classObject);
    Object constructor = objectLibrary.getOrDefault(classObject, classObject.name, null);
    if(constructor instanceof NFunctionObject constructorFunction) {
      constructorDispatch.executeDispatch(constructorFunction, object, executeArguments(frame));
    } else {
      consumeArguments(frame);
    }
    return object;
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

  @ExplodeLoop
  private Object[] executeArguments(VirtualFrame frame) {
    Object[] args = new Object[arguments.length];
    for(int i = 0; i < arguments.length; i++) {
      args[i] = arguments[i].execute(frame);
    }
    return args;
  }
}
