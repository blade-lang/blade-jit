package org.blade.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

@ExportLibrary(InteropLibrary.class)
public class BObject extends BladeClass {
  public BObject(Shape shape) {
    super(
      shape, "Object", new DynamicObject(shape) {
      }
    );
  }

  @ExportMessage
  boolean isMemberReadable(String member, @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return objectLibrary.containsKey(this, member);
  }

  @ExportMessage
  Object readMember(String member, @CachedLibrary("this") DynamicObjectLibrary objectLibrary)
    throws UnknownIdentifierException {
    Object value = objectLibrary.getOrDefault(this, member, null);
    if (value == null) {
      throw UnknownIdentifierException.create(member);
    }
    return value;
  }

  @ExportMessage
  boolean isMemberModifiable(String member, @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return isMemberReadable(member, objectLibrary);
  }

  @ExportMessage
  boolean isMemberInsertable(String member, @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return !isMemberModifiable(member, objectLibrary);
  }
}
