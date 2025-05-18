package org.blade.language.nodes;


import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.nodes.functions.NMethodDispatchNode;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;
import org.blade.language.runtime.*;

import java.math.BigInteger;

@NodeChild("leftNode")
@NodeChild("rightNode")
public abstract class NBinaryNode extends NNode {
  protected static boolean isDouble(Object object) {
    return object instanceof Double;
  }

  protected static boolean isLong(Object object) {
    return object instanceof Long;
  }

  @CompilerDirectives.TruffleBoundary
  protected static long bigToLong(BigInteger object) {
    return object.intValue();
  }

  @CompilerDirectives.TruffleBoundary
  protected static int bigToInt(BigInteger object) {
    return object.intValue();
  }

  protected static boolean isList(Object object) {
    return object instanceof ListObject;
  }

  protected static Object methodOverride(Node node, String def, BladeObject left, BladeObject right, InteropLibrary interopLibrary) {
    Object overrideFunction = null;
    try {
      overrideFunction = interopLibrary.readMember(left, def);
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.error(node, e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if(overrideFunction instanceof FunctionObject function) {
      return getDispatchNode().executeDispatch(function, left, new Object[]{right});
    }

    return null;
  }

  @CompilerDirectives.TruffleBoundary
  private static NMethodDispatchNode getDispatchNode() {
    return NMethodDispatchNodeGen.create();
  }
}
