package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

import java.util.ArrayList;
import java.util.List;

@ExportLibrary(InteropLibrary.class)
public class NListObject extends NimObject {
  static final String LENGTH_PROP = "length";

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  public Object[] items;

  // List properties...
  @DynamicField private int length;

  public NListObject(Shape shape, NimClass classObject, Object[] objects) {
    super(shape, classObject);
    setArrayElements(objects, DynamicObjectLibrary.getUncached());
  }

  @ExportMessage
  boolean hasArrayElements() {
    return true;
  }

  @ExportMessage
  public long getArraySize() {
    return items.length;
  }

  @ExportMessage
  public boolean isArrayElementReadable(long index) {
    int length = items.length;
    index = effectiveIndex(index, length);
    return index < length && index >= 0;
  }

  @ExportMessage
  boolean isArrayElementModifiable(long index) {
    return isArrayElementReadable(index);
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
      : NimNil.SINGLETON;
  }

//  static class ReadArrayElement {
//    @Specialization(guards = {"index < length", "index >= 0"})
//    static Object doWithinLength(NListObject list, long index,
//                               @Cached(value = "list.items.length", neverDefault = true) @Cached.Shared("length") int length) {
//      return list.readArrayElement(index);
//    }
//
//    @Specialization(guards = {"index < 0"})
//    static Object doIndexLessThanZero(NListObject list, long index,
//                                    @Cached(value = "list.items.length", neverDefault = true) @Cached.Shared("length") int length) {
//      return list.readArrayElement(index + length);
//    }
//
//    @Fallback
//    static Object doInvalid(NListObject list, long index) {
//      return NimNil.SINGLETON;
//    }
//  }

  @ExportMessage
  Object readMember(String member,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary,
                    @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary) throws UnsupportedMessageException, UnknownIdentifierException {
    return switch (member) {
      case "length" -> objectLibrary.getOrDefault(this, "length", 0);
      default -> super.readMember(member, objectLibrary, classInteropLibrary);
    };
  }

  @ExportMessage.Ignore
  void writeArrayElement(long index, Object value) {
    items[(int)index] = value;
  }

  @ExportMessage
  static class WriteArrayElement {
    @Specialization(guards = {"index < length", "index >= 0"})
    static void doWithinLength(NListObject list, long index, Object value,
                               @Cached(value = "list.items.length", neverDefault = true) @Cached.Shared("length") int length) {
      list.writeArrayElement(index, value);
    }

    @Specialization(guards = {"index > 0", "index < list.items.length"})
    static void doWithinLengthUncached(NListObject list, long index, Object value) {
      list.writeArrayElement(index, value);
    }

    @Specialization(guards = {"index < 0"})
    static void doIndexLessThanZero(NListObject list, long index, Object value,
                                    @Cached(value = "list.items.length", neverDefault = true) @Cached.Shared("length") int length) {
      list.writeArrayElement(index + list.items.length, value);
    }

    @Fallback
    static void doInvalid(NListObject list, long index, Object value) {
      throw NimRuntimeError.create("List index ", index, " out of range");
    }
  }

  @ExportMessage
  static class WriteMember {
    @Specialization(guards = "LENGTH_PROP.equals(member)")
    static void writeLength(NListObject list, String member, Object value) {
      throw NimRuntimeError.create("Direct modification of list.length prohibited");
    }

    @Fallback
    static void writeNonLength(
      NListObject list, String member, Object value,
      @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary
    ) {
      list.writeMember(member, value, objectLibrary);
    }
  }

  @ExplodeLoop
  @Override
  public String toString() {
    List<String> builder = new ArrayList<>();
    for(Object item : items) {
      builder.add(NString.toString(item));
    }

    String result = "[" + NString.join(", ", builder) + "]";
    builder.clear();

    return result;
  }

  private void setArrayElements(Object[] items, DynamicObjectLibrary objectLibrary) {
    this.items = items;
    writeMember(LENGTH_PROP, items.length, objectLibrary);
  }

  @ExplodeLoop
  public void resize(long length, DynamicObjectLibrary objectLibrary) {
    Object[] newItems = new Object[(int) length];
    for (int i = 0; i < length; i++) {
      newItems[i] = i < this.items.length
        ? this.items[i]
        : NimNil.SINGLETON;
    }
    this.setArrayElements(newItems, objectLibrary);
  }

  private long effectiveIndex(long index, int length) {
    return index + (length & -(index >> 31));
  }
}
