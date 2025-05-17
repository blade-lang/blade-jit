package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import org.blade.annotations.ObjectName;

@ExportLibrary(InteropLibrary.class)
@ObjectName("Class")
public class BladeClass extends BladeObject {
  public final String name;
  public final boolean isBuiltin;

  public BladeClass(Shape shape, String name, DynamicObject classObject) {
    this(shape, name, classObject, false);
  }

  public BladeClass(Shape shape, String name, DynamicObject classObject, boolean isBuiltin) {
    super(shape, classObject);
    this.name = name;
    this.isBuiltin = isBuiltin;
  }

  @CompilerDirectives.TruffleBoundary
  @Override
  public String toString() {
    return "<class " + name + ">";
  }

  @CompilerDirectives.TruffleBoundary
  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return toString();
  }

  @ExportMessage
  Object getMetaObject() throws UnsupportedMessageException {
    return BladeType.CLASS;
  }
}
