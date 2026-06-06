package org.zuri.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.nodes.NNode;
import org.zuri.language.runtime.ZuriNil;
import org.zuri.language.runtime.ZuriObject;
import org.zuri.language.runtime.ZuriRuntimeError;
import org.zuri.language.runtime.FunctionObject;

@NodeChild(value = "arguments", type = NReadFunctionArgsExprNode[].class)
@GenerateNodeFactory
public abstract class NBuiltinFunctionNode extends NNode {
  protected static Object methodOverride(Node node, String def, ZuriObject object, InteropLibrary interopLibrary, Object defaultValue) {
    Object overrideFunction = null;
    try {
      overrideFunction = interopLibrary.readMember(object, def);
    } catch (UnsupportedMessageException e) {
      throw ZuriRuntimeError.error(node, e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if (overrideFunction instanceof FunctionObject function) {
      return getDispatchNode().executeDispatch(function, object, new Object[0]);
    }

    return defaultValue;
  }

  protected static Object methodOverride(Node node, String def, ZuriObject object, InteropLibrary interopLibrary) {
    return methodOverride(node, def, object, interopLibrary, ZuriNil.SINGLETON);
  }

  @CompilerDirectives.TruffleBoundary
  private static NMethodDispatchNode getDispatchNode() {
    return NMethodDispatchNodeGen.create();
  }

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
}
