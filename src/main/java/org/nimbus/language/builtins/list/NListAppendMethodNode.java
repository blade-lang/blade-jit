package org.nimbus.language.builtins.list;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NListAppendMethodNode extends NBuiltinFunctionNode {
  @Specialization
  protected Object doAny(DynamicObject self, Object item,
                         @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary,
                          @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
    // strings only have the 'length' item
    try {
      NListObject list = (NListObject) self;
      long size = list.getArraySize();
      list.resize(size + 1, objectLibrary);
      interopLibrary.writeArrayElement(self, size, item);
    } catch (UnsupportedMessageException | UnsupportedTypeException | InvalidArrayIndexException e) {
      throw NimRuntimeError.create(e.getMessage());
    }
    return item;
  }
}
