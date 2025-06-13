package org.blade.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeNil;
import org.blade.language.runtime.BladeObject;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.FunctionObject;

@NodeChild(value = "arguments", type = NReadFunctionArgsExprNode[].class)
@GenerateNodeFactory
public abstract class NBuiltinFunctionNode extends NNode {
  protected boolean isDouble(Object object) {
    return object instanceof Double;
  }

  protected boolean isLong(Object object) {
    return object instanceof Long;
  }

  protected boolean isString(Object object) {
    return object instanceof TruffleString;
  }

  @CompilerDirectives.TruffleBoundary
  protected boolean objectEquals(Object first, Object second) {
    return first.equals(second);
  }

  protected static Object methodOverride(Node node, String def, BladeObject object, InteropLibrary interopLibrary, Object defaultValue) {
    Object overrideFunction = null;
    try {
      overrideFunction = interopLibrary.readMember(object, def);
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.error(node, e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if (overrideFunction instanceof FunctionObject function) {
      return getDispatchNode().executeDispatch(function, object, new Object[0]);
    }

    return defaultValue;
  }

  protected static Object methodOverride(Node node, String def, BladeObject object, InteropLibrary interopLibrary) {
    return methodOverride(node, def, object, interopLibrary, BladeNil.SINGLETON);
  }

  @CompilerDirectives.TruffleBoundary
  private static NMethodDispatchNode getDispatchNode() {
    return NMethodDispatchNodeGen.create();
  }
}
