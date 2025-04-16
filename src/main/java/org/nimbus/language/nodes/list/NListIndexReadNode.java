package org.nimbus.language.nodes.list;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.NimRuntimeError;

@NodeChild("listExpr")
@NodeChild("indexExpr")
public abstract class NListIndexReadNode extends NNode {
  @Specialization(guards = "listLibrary.isArrayElementReadable(list, index)", limit = "3")
  protected Object doLong(Object list, long index,
                          @CachedLibrary("list") InteropLibrary listLibrary) {
    try {
      return listLibrary.readArrayElement(list, index);
    } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
      throw new NimRuntimeError(e.getMessage());
    }
  }

  @Specialization(guards = "listLibrary.isNull(list)", limit = "3")
  protected Object doNil(Object list, long index,
                         @CachedLibrary("list") InteropLibrary listLibrary) {
    throw new NimRuntimeError("Cannot read properties of nil (reading '" + index + "')");
  }

  @Fallback
  protected Object doUnsupported(Object array, Object index) {
    if(index instanceof Long || index instanceof Double) {
      throw new NimRuntimeError("List index " + index + " out of range");
    }
    throw new NimRuntimeError("Lists are numerically indexed");
  }
}
