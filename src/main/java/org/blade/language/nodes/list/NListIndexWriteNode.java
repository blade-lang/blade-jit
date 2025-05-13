package org.blade.language.nodes.list;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NSharedPropertyWriterNode;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.ListObject;

@NodeChild("listExpr")
@NodeChild("indexExpr")
@NodeChild("valueExpr")
@ImportStatic(BString.class)
public abstract class NListIndexWriteNode extends NNode {
  @Specialization(guards = "listLibrary.isArrayElementWritable(list, index)", limit = "3")
  protected Object doLong(Object list, long index, Object value,
                          @CachedLibrary("list") InteropLibrary listLibrary) {
    try {
      listLibrary.writeArrayElement(list, index, value);
    } catch (UnsupportedMessageException | InvalidArrayIndexException | UnsupportedTypeException e) {
      throw BladeRuntimeError.create(e.getMessage());
    }

    return value;
  }

  @Specialization(guards = "equals(name, cachedName, equalNode)", limit = "3")
  protected Object doStringCached(
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
  protected Object doString(
    Object target, TruffleString name, Object value,
    @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
    @Cached @Cached.Shared("sharedPropertyWriterNode") NSharedPropertyWriterNode sharedPropertyWriterNode
  ) {
    return sharedPropertyWriterNode.executeWrite(target, toJavaStringNode.execute(name), value);
  }

  @Specialization(guards = {"isBool(list)"}, limit = "3")
  protected Object doBool(
    Object list, long index, Object value,
    @CachedLibrary("list") InteropLibrary listLibrary
  ) {
    throw BladeRuntimeError.create("Cannot set properties of nil (reading '" , index, "')", this);
  }

  @Specialization(guards = {"listLibrary.isNull(list)"}, limit = "3")
  protected Object doNil(
    Object list, long index, Object value,
    @CachedLibrary("list") InteropLibrary listLibrary
  ) {
    throw BladeRuntimeError.create("Cannot set properties of boolean value (reading '" , index, "')", this);
  }

  @Fallback
  protected Object doNonStringProperty(
    Object target, Object index, Object value,
    @Cached @Cached.Shared("sharedPropertyWriterNode") NSharedPropertyWriterNode sharedPropertyWriterNode
  ) {
    if(target instanceof ListObject && (index instanceof Long || index instanceof Double)) {
      throw BladeRuntimeError.create("List index ", index," out of range");
    }

    return sharedPropertyWriterNode.executeWrite(target, BString.toString(index), value);
  }

  protected boolean isBool(Object value) {
    return value instanceof Boolean;
  }
}
