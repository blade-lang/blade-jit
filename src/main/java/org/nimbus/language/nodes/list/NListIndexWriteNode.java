package org.nimbus.language.nodes.list;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimRuntimeError;

@NodeChild("listExpr")
@NodeChild("indexExpr")
@NodeChild("valueExpr")
public abstract class NListIndexWriteNode extends NNode {
  @Specialization(guards = "listLibrary.isArrayElementWritable(list, index)", limit = "3")
  protected Object doLong(Object list, long index, Object value,
                          @CachedLibrary("list") InteropLibrary listLibrary) {
    try {
      listLibrary.writeArrayElement(list, index, value);
    } catch (UnsupportedMessageException | InvalidArrayIndexException | UnsupportedTypeException e) {
      throw new NimRuntimeError(e.getMessage());
    }

    return value;
  }

  @Specialization(guards = "listLibrary.isNull(list)", limit = "3")
  protected Object doNil(Object list, long index, Object value,
                         @CachedLibrary("list") InteropLibrary listLibrary) {
    throw new NimRuntimeError("Cannot set properties of nil (reading '" + index + "')");
  }

  @Fallback
  protected Object doUnsupported(Object array, Object index, Object value) {
    if(index instanceof Long || index instanceof Double) {
      throw new NimRuntimeError("List index " + index + " out of range");
    }
    throw new NimRuntimeError("Lists are numerically indexed");
//    return RemNil.SINGLETON;
  }
}
