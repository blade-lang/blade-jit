package org.zuri.language.nodes.list;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.common.NPropertyWriterNode;
import org.zuri.language.runtime.ZString;
import org.zuri.language.runtime.ZuriRuntimeError;
import org.zuri.language.runtime.ListObject;

@NodeChild("listExpr")
@NodeChild("indexExpr")
@NodeChild("valueExpr")
@ImportStatic(ZString.class)
public abstract class NWriteListIndexNode extends NNode {
  @Specialization(guards = "listLibrary.isArrayElementWritable(list, index)", limit = "3")
  protected static Object doLong(Object list, long index, Object value, @Bind Node node,
                          @CachedLibrary("list") InteropLibrary listLibrary) {
    try {
      listLibrary.writeArrayElement(list, index, value);
    } catch (UnsupportedMessageException | InvalidArrayIndexException | UnsupportedTypeException e) {
      throw ZuriRuntimeError.error(node, e.getMessage());
    }

    return value;
  }

  @Specialization(guards = "equals(name, cachedName, equalNode)", limit = "3")
  protected static Object doStringCached(
    Object target, TruffleString name, Object value,
    @Cached("name") TruffleString cachedName,
    @Cached TruffleString.EqualNode equalNode,
    @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
    @Cached("toJavaStringNode.execute(name)") String javaPropertyName,
    @Cached @Cached.Shared("sharedPropertyWriterNode") NPropertyWriterNode sharedPropertyWriterNode
  ) {
    return sharedPropertyWriterNode.executeWrite(target, javaPropertyName, value);
  }

  @Specialization(replaces = "doStringCached")
  protected static Object doString(
    Object target, TruffleString name, Object value,
    @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
    @Cached @Cached.Shared("sharedPropertyWriterNode") NPropertyWriterNode sharedPropertyWriterNode
  ) {
    return sharedPropertyWriterNode.executeWrite(target, toJavaStringNode.execute(name), value);
  }

  @Specialization(guards = {"isBool(list)"}, limit = "3")
  protected static Object doBool(
    Object list, long index, Object value, @Bind Node node,
    @CachedLibrary("list") InteropLibrary listLibrary
  ) {
    throw ZuriRuntimeError.error(node, "Cannot set properties of nil (reading '", index, "')");
  }

  @Specialization(guards = {"listLibrary.isNull(list)"}, limit = "3")
  protected static Object doNil(
    Object list, long index, Object value, @Bind Node node,
    @CachedLibrary("list") InteropLibrary listLibrary
  ) {
    throw ZuriRuntimeError.error(node, "Cannot set properties of boolean value (reading '", index, "')");
  }

  @Fallback
  protected static Object doNonStringProperty(
    Object target, Object index, Object value, @Bind Node node,
    @Cached @Cached.Shared("sharedPropertyWriterNode") NPropertyWriterNode sharedPropertyWriterNode
  ) {
    if (target instanceof ListObject && (index instanceof Long || index instanceof Double)) {
      throw ZuriRuntimeError.error(node, "List index ", index, " out of range");
    }

    return sharedPropertyWriterNode.executeWrite(target, ZString.toString(index), value);
  }

  protected static boolean isBool(Object value) {
    return value instanceof Boolean;
  }
}
