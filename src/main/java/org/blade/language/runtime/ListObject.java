package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import java.util.ArrayList;
import java.util.List;

@ExportLibrary(InteropLibrary.class)
public final class ListObject extends BladeObject {
  @CompilerDirectives.CompilationFinal(dimensions = 1)
  private Object[] items;

  public ListObject(Shape shape, BladeClass classObject, Object[] objects) {
    super(shape, classObject);
    this.items = objects;
  }

  public Object[] getItems() {
    return items;
  }

  public ListObject getSlice(int start, int end) {
    int effectiveLength = end - start;
    Object[] objects = new Object[effectiveLength];
    System.arraycopy(items, start, objects, 0, effectiveLength);
    return new ListObject(getShape(), (BladeClass) classObject, objects);
  }

  @ExportMessage
  boolean hasArrayElements() {
    return true;
  }

  @ExportMessage
  public long getArraySize() {
    return items.length;
  }

  @ExportMessage(name = "isArrayElementReadable")
  @ExportMessage(name = "isArrayElementModifiable")
  public boolean isArrayElementReadable(long index) {
    final long length = items.length;
    index = effectiveIndex(index, length);
    return index < length && index >= 0;
  }

  @ExportMessage
  boolean isArrayElementInsertable(long index) {
    return false;
  }

  @ExportMessage
  Object readArrayElement(long index) {
    index = effectiveIndex(index, items.length);

    return isArrayElementReadable(index)
      ? items[(int) index]
      : BladeNil.SINGLETON;
  }

  @ExportMessage.Ignore
  void writeArrayElement(long index, Object value) {
    items[(int) index] = value;
  }

  @Override
  public String toString() {
    List<String> builder = new ArrayList<>();
    for (Object item : items) {
      builder.add(BString.toString(item));
    }

    String result = "[" + BString.join(", ", builder) + "]";
    builder.clear();

    return result;
  }

  @ExplodeLoop
  public void resize(long length) {
    final int itemsLength = items.length;
    Object[] newItems = new Object[(int) length];
    for (int i = 0; i < length; i++) {
      newItems[i] = i < itemsLength
        ? this.items[i]
        : BladeNil.SINGLETON;
    }
    this.items = newItems;
  }

  private long effectiveIndex(long index, long length) {
    if (index < 0) {
      return index + length;
    }
    return index;
  }

  @ExportMessage
  static class WriteArrayElement {
    @Specialization(guards = {"index < length", "index >= 0"})
    static void doWithinLength(ListObject list, long index, Object value,
                               @Cached(value = "list.getArraySize()", allowUncached = true, neverDefault = true) @Cached.Shared("length") long length) {
      list.writeArrayElement(index, value);
    }

    @Specialization(guards = {"index > 0", "index < list.getArraySize()"})
    static void doWithinLengthUncached(ListObject list, long index, Object value) {
      list.writeArrayElement(index, value);
    }

    @Specialization(guards = {"index < 0"})
    static void doIndexLessThanZero(ListObject list, long index, Object value,
                                    @Cached(value = "list.getArraySize()", allowUncached = true, neverDefault = true) @Cached.Shared("length") long length) {
      list.writeArrayElement(index + list.getArraySize(), value);
    }

    @Fallback
    static void doInvalid(ListObject list, long index, Object value, @Bind Node node) {
      throw BladeRuntimeError.error(node, "List index ", index, " out of range");
    }
  }

  @ExportMessage
  static class WriteMember {
    @Specialization
    static void writeNonLength(
      ListObject list, String member, Object value,
      @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary
    ) {
      list.writeMember(member, value, objectLibrary);
    }
  }
}
