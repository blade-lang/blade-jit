package org.nimbus.language.nodes.list;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NSharedPropertyReaderNode;
import org.nimbus.language.nodes.NimTypesGen;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NString;
import org.nimbus.language.runtime.NimRuntimeError;

@NodeChild("listExpr")
@NodeChild("indexExpr")
@ImportStatic(NString.class)
public abstract class NListIndexReadNode extends NNode {
  @Specialization(guards = "listLibrary.isArrayElementReadable(list, index)", limit = "3")
  protected Object doListLong(Object list, long index,
                          @CachedLibrary("list") InteropLibrary listLibrary) {
    try {
      return listLibrary.readArrayElement(list, index);
    } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
      throw new NimRuntimeError(e.getMessage());
    }
  }

  @Specialization(guards = "equals(property, cachedProperty, equalNode)", limit = "3")
  protected Object doListStringCached(
    Object list, TruffleString property,
    @Cached("property") TruffleString cachedProperty,
    @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
    @Cached("toJavaStringNode.execute(cachedProperty)") String cachedJavaString,
    @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode,
    @Cached TruffleString.EqualNode equalNode
  ) {
    return propertyReaderNode.executeRead(list, cachedJavaString);
  }

  @Specialization(replaces = "doListStringCached")
  protected Object doListString(
    Object list, TruffleString property,
    @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
    @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode
  ) {
    return propertyReaderNode.executeRead(list, toJavaStringNode.execute(property));
  }

  @Specialization(guards = "listLibrary.isNull(list)", limit = "3")
  protected Object doNil(Object list, long index,
                         @CachedLibrary("list") InteropLibrary listLibrary) {
    throw new NimRuntimeError("Cannot read properties of nil (reading '" + index + "')");
  }

  @Fallback
  protected Object doUnsupported(
    Object list, Object index,
    @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode
  ) {
    if(list instanceof NListObject && NimTypesGen.isImplicitDouble(index)) {
      throw new NimRuntimeError("List index " + index +" out of range");
    }
    return propertyReaderNode.executeRead(list, index);
  }
}
