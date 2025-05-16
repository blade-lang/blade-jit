package org.blade.language.nodes;


import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import org.blade.language.nodes.functions.NMethodDispatchNode;
import org.blade.language.nodes.functions.NMethodDispatchNodeGen;
import org.blade.language.runtime.*;

import java.math.BigInteger;

@NodeChild("leftNode")
@NodeChild("rightNode")
public abstract class NBinaryNode extends NNode {

  @Child
  @SuppressWarnings("FieldMayBeFinal")
  private NMethodDispatchNode dispatchNode = NMethodDispatchNodeGen.create();

  protected boolean isDouble(Object object) {
    return object instanceof Double;
  }

  protected boolean isLong(Object object) {
    return object instanceof Long;
  }

  @CompilerDirectives.TruffleBoundary
  protected long bigToLong(BigInteger object) {
    return object.intValue();
  }

  @CompilerDirectives.TruffleBoundary
  protected int bigToInt(BigInteger object) {
    return object.intValue();
  }

  protected boolean isList(Object object) {
    return object instanceof ListObject;
  }

  protected Object methodOverride(String def, BladeObject left, BladeObject right, InteropLibrary interopLibrary) {
    Object overrideFunction = null;
    try {
      overrideFunction = interopLibrary.readMember(left, def);
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.error(this, e.getMessage());
    } catch (UnknownIdentifierException e) {
      // fallthrough
    }

    if(overrideFunction instanceof FunctionObject function) {
      return dispatchNode.executeDispatch(function, left, new Object[]{right});
    }

    return null;
  }
}
