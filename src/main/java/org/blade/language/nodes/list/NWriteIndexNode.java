package org.blade.language.nodes.list;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NSharedPropertyWriterNode;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.ListObject;

@GenerateInline
@ImportStatic(BString.class)
public abstract class NWriteIndexNode extends Node {

  public static NWriteIndexNode getUncached() {
    return NWriteIndexNodeGen.getUncached();
  }

  @NeverDefault
  public static NWriteIndexNode create() {
    return NWriteIndexNodeGen.create();
  }

  @Specialization(guards = "listLibrary.isArrayElementWritable(list, index)", limit = "3")
  protected static Object doLong(Node node, Object list, long index, Object value,
                                 @CachedLibrary("list") InteropLibrary listLibrary) {
    try {
      listLibrary.writeArrayElement(list, index, value);
    } catch (UnsupportedMessageException | InvalidArrayIndexException | UnsupportedTypeException e) {
      throw BladeRuntimeError.error(node, e.getMessage());
    }

    return value;
  }

  @Specialization(guards = "equals(name, cachedName, equalNode)", limit = "3")
  protected static Object doStringCached(Node node,
                                         Object target, TruffleString name, Object value,
                                         @Cached("name") TruffleString cachedName,
                                         @Cached TruffleString.EqualNode equalNode,
                                         @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
                                         @Cached("toJavaStringNode.execute(name)") String javaPropertyName,
                                         @Cached @Cached.Shared("sharedPropertyWriterNode") NSharedPropertyWriterNode sharedPropertyWriterNode
  ) {
    return sharedPropertyWriterNode.executeWrite(target, javaPropertyName, value);
  }

  @Specialization(replaces = "doStringCached")
  protected static Object doString(Node node,
                                   Object target, TruffleString name, Object value,
                                   @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
                                   @Cached @Cached.Shared("sharedPropertyWriterNode") NSharedPropertyWriterNode sharedPropertyWriterNode
  ) {
    return sharedPropertyWriterNode.executeWrite(target, toJavaStringNode.execute(name), value);
  }

  @Specialization(guards = {"isBool(list)"}, limit = "3")
  protected static Object doBool(Node node,
                                 Object list, long index, Object value,
                                 @CachedLibrary("list") InteropLibrary listLibrary
  ) {
    throw BladeRuntimeError.error(node, "Cannot set properties of nil (reading '", index, "')");
  }

  @Specialization(guards = {"listLibrary.isNull(list)"}, limit = "3")
  protected static Object doNil(Node node,
                                Object list, long index, Object value,
                                @CachedLibrary("list") InteropLibrary listLibrary
  ) {
    throw BladeRuntimeError.error(node, "Cannot set properties of boolean value (reading '", index, "')");
  }

  @Fallback
  protected static Object doNonStringProperty(Node node,
                                              Object target, Object index, Object value,
                                              @Cached @Cached.Shared("sharedPropertyWriterNode") NSharedPropertyWriterNode sharedPropertyWriterNode
  ) {
    if (target instanceof ListObject && (index instanceof Long || index instanceof Double)) {
      throw BladeRuntimeError.error(node, "List index ", index, " out of range");
    }

    return sharedPropertyWriterNode.executeWrite(target, BString.toString(index), value);
  }

  public abstract Object executeWrite(Node node, Object list, Object index, Object value);

  protected boolean isBool(Object value) {
    return value instanceof Boolean;
  }
}
