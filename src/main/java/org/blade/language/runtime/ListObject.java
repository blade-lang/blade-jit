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
public class ListObject extends BladeObject {
  static final String LENGTH_PROP = "length";

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  public Object[] items;

  // List properties...
  @DynamicField private long length;

  public ListObject(Shape shape, BladeClass classObject, Object[] objects) {
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
  public boolean isArrayElementReadable(long index,
                                        @Bind Node node,
                                        @Cached @Cached.Shared("profile") InlinedConditionProfile profile) {
    long length = items.length;
    index = effectiveIndex(node, profile, index, length);
    return index < length && index >= 0;
  }

  @ExportMessage
  boolean isArrayElementModifiable(long index, @Bind Node node, @Cached @Cached.Shared("profile") InlinedConditionProfile profile) {
    return isArrayElementReadable(index, node, profile);
  }

  @ExportMessage
  boolean isArrayElementInsertable(long index) {
    return false;
  }

  @ExportMessage
  Object readArrayElement(long index, @Bind Node node, @Cached @Cached.Shared("profile") InlinedConditionProfile profile) {
    index = effectiveIndex(node, profile, index, items.length);

    return isArrayElementReadable(index, node, profile)
      ? items[(int) index]
      : BladeNil.SINGLETON;
  }

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
    static void doWithinLength(ListObject list, long index, Object value,
                               @Cached(value = "list.items.length", neverDefault = true) @Cached.Shared("length") int length) {
      list.writeArrayElement(index, value);
    }

    @Specialization(guards = {"index > 0", "index < list.items.length"})
    static void doWithinLengthUncached(ListObject list, long index, Object value) {
      list.writeArrayElement(index, value);
    }

    @Specialization(guards = {"index < 0"})
    static void doIndexLessThanZero(ListObject list, long index, Object value,
                                    @Cached(value = "list.items.length", neverDefault = true) @Cached.Shared("length") int length) {
      list.writeArrayElement(index + list.items.length, value);
    }

    @Fallback
    static void doInvalid(ListObject list, long index, Object value) {
      throw BladeRuntimeError.create("List index ", index, " out of range");
    }
  }

  @ExportMessage
  static class WriteMember {
    @Specialization(guards = "LENGTH_PROP.equals(member)")
    static void writeLength(ListObject list, String member, Object value) {
      throw BladeRuntimeError.create("Direct modification of list.length prohibited");
    }

    @Fallback
    static void writeNonLength(
      ListObject list, String member, Object value,
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
      builder.add(BString.toString(item));
    }

    String result = "[" + BString.join(", ", builder) + "]";
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
        : BladeNil.SINGLETON;
    }
    this.setArrayElements(newItems, objectLibrary);
  }

  private long effectiveIndex(Node node, InlinedConditionProfile profile, long index, long length) {
    if(profile.profile(node, index < 0)) {
      return index + length;
    }
    return index;
  }
}
