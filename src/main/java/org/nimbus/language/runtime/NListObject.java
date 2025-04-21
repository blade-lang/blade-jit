package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
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
  private Object[] items;

  // List properties...
  @DynamicField private long length;

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
    index = effectiveIndex(index);
    return index >= 0 && index < items.length;
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
    index = effectiveIndex(index);

    // TODO: Throw proper error as defined in Blade spec.
    return this.isArrayElementReadable(index)
      ? items[(int) index]
      : NimNil.SINGLETON;
  }

  @ExportMessage
  Object readMember(String member,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary) throws UnknownIdentifierException {
    return switch (member) {
      case "length" -> objectLibrary.getOrDefault(this, "length", 0);
      default -> throw UnknownIdentifierException.create(member);
    };
  }

  @ExportMessage
  void writeArrayElement(long index, Object value) {
    index = effectiveIndex(index);

    if (this.isArrayElementModifiable(index)) {
      items[(int) index] = value;
    } else {
      throw new NimRuntimeError("List index " + index + " out of range");
    }
  }

  @ExportMessage
  static class WriteMember {
    @Specialization(guards = "LENGTH_PROP.equals(member)")
    static void writeLength(NListObject list, String member, Object value) {
      throw new NimRuntimeError("Direct modification of list.length prohibited");
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

  private long effectiveIndex(long index) {
    if(index < 0) index = items.length + index;
    return (int)index;
  }
}
