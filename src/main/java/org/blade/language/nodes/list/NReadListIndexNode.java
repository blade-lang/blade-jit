package org.blade.language.nodes.list;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.BladeTypesGen;
import org.blade.language.nodes.NSharedPropertyReaderNode;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.ListObject;

@GenerateInline
@ImportStatic(BString.class)
public abstract class NReadListIndexNode extends Node {

  public static NReadListIndexNode getUncached() {
    return NReadListIndexNodeGen.getUncached();
  }

  @NeverDefault
  public static NReadListIndexNode create() {
    return NReadListIndexNodeGen.create();
  }

  @Specialization(guards = "listLibrary.isArrayElementReadable(list, index)", limit = "3")
  protected static Object doListLong(Node node, Object list, long index,
                                     @CachedLibrary("list") InteropLibrary listLibrary) {
    try {
      return listLibrary.readArrayElement(list, index);
    } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
      throw BladeRuntimeError.error(node, e.getMessage());
    }
  }

  @Specialization(guards = "equals(property, cachedProperty, equalNode)", limit = "3")
  protected static Object doListStringCached(Node node,
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
  protected static Object doListString(Node node,
                                       Object list, TruffleString property,
                                       @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
                                       @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode
  ) {
    return propertyReaderNode.executeRead(list, toJavaStringNode.execute(property));
  }

  @Specialization(guards = "listLibrary.isNull(list)", limit = "3")
  protected static Object doNil(Node node, Object list, long index,
                                @CachedLibrary("list") InteropLibrary listLibrary) {
    throw BladeRuntimeError.error(node, "Cannot read properties of nil (reading '", index, "')");
  }

  @Specialization(guards = "interopLibrary.hasMembers(list)", limit = "3")
  protected static Object doNonString(Node node,
                                      Object list, Object property,
                                      @CachedLibrary("list") InteropLibrary interopLibrary,
                                      @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode
  ) {
    return propertyReaderNode.executeRead(list, BString.toString(property));
  }

  @Fallback
  protected static Object doUnsupported(Node node,
                                        Object list, Object index,
                                        @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode
  ) {
    if (list instanceof ListObject && BladeTypesGen.isImplicitDouble(index)) {
      throw BladeRuntimeError.error(node, "List index ", index, " out of range");
    }
    return propertyReaderNode.executeRead(list, index);
  }

  public abstract Object executeRead(Node node, Object list, Object index);
}
