package org.blade.language.nodes;


import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;
import org.blade.language.runtime.*;

@NodeChild("leftNode")
@NodeChild("rightNode")
public abstract class NBinaryNode extends NNode {
  protected static boolean isDouble(Object object) {
    return object instanceof Double;
  }

  protected static boolean isLong(Object object) {
    return object instanceof Long;
  }

  protected static boolean isList(Object object) {
    return object instanceof ListObject;
  }

  protected static Object methodOverride(String def, BladeObject left, BladeObject right, InteropLibrary interopLibrary) {
    Object overrideFunction = null;
    try {
      overrideFunction = interopLibrary.readMember(left, def);
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.create(e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if(overrideFunction instanceof FunctionObject function) {
      return NMethodDispatchNodeGen.create().executeDispatch(function, left, new Object[]{right});
    }

    return null;
  }
}
