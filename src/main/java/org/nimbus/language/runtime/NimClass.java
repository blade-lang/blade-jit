package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.annotations.ObjectName;

@ExportLibrary(InteropLibrary.class)
@ObjectName("Class")
public class NimClass extends NimObject {
  public final String name;

  public NimClass(Shape shape, String name, DynamicObject classObject) {
    super(shape, classObject);
    this.name = name;
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
    return NimType.CLASS;
  }
}
