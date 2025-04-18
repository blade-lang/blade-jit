package org.nimbus.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.annotations.ObjectName;

import java.util.ArrayList;
import java.util.List;

@ExportLibrary(InteropLibrary.class)
@ObjectName("List")
public class NListObject extends NBaseObject {
  private Object[] items;

  // List properties...
  @DynamicField private long length;

  public NListObject(Shape shape, Object[] objects) {
    super(shape);
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
//    return index >= items.length;
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
  void writeArrayElement(long index, Object value) {
    index = effectiveIndex(index);

    if (this.isArrayElementModifiable(index)) {
      items[(int) index] = value;
    } else {
      throw new NimRuntimeError("List index " + index + " out of range");
    }
  }

  @ExportMessage
  Object readMember(String member,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary) throws UnknownIdentifierException {
    switch (member) {
      case "length": return objectLibrary.getOrDefault(this, "length", 0);
      default: throw UnknownIdentifierException.create(member);
    }
  }

  @Override
  public String toString() {
    List<String> builder = new ArrayList<>();
    for(Object item : items) {
      builder.add(item.toString());
    }

    String result = "[" + String.join(", ", builder) + "]";
    builder.clear();

    return result;
  }

  private void setArrayElements(Object[] items, DynamicObjectLibrary objectLibrary) {
    this.items = items;
    objectLibrary.putLong(this, "length", items.length);
  }

  private long effectiveIndex(long index) {
    if(index < 0) index = items.length + index;
    return (int)index;
  }
}
