package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import org.blade.annotations.ObjectName;

@ExportLibrary(InteropLibrary.class)
@ObjectName("Range")
public final class RangeObject extends BladeObject {
  public final long lower;
  public final long upper;
  public final long range;

  public RangeObject(Shape shape, DynamicObject classObject, long lower, long upper) {
    super(shape, classObject);
    this.lower = lower;
    this.upper = upper;
    this.range = upper > lower ? upper - lower : lower - upper;
  }

//  @ExportMessage
//  boolean isMemberReadable(String member, @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
//    return member.equals("upper") || member.equals("lower") || member.equals("range");
//  }

  @ExportMessage
  Object readMember(String member,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary,
                    @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary) throws UnsupportedMessageException, UnknownIdentifierException {
    return switch(member) {
      case "upper" -> upper;
      case "lower" -> lower;
      case "range" -> range;
      default -> super.readMember(member, objectLibrary, classInteropLibrary);
    };
  }

  @ExportMessage
  boolean isMemberModifiable(String member) {
    return false;
  }

  @ExportMessage
  boolean isMemberInsertable(String member) {
    return false;
  }

  @CompilerDirectives.TruffleBoundary
  @Override
  public String toString() {
    return "<range " + lower + ".." + upper + ">";
  }

  @CompilerDirectives.TruffleBoundary
  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return toString();
  }
}
