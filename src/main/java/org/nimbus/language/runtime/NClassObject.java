package org.nimbus.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.annotations.ObjectName;

@ExportLibrary(InteropLibrary.class)
@ObjectName("Class")
public final class NClassObject extends NBaseObject {
  public final String name;

  public NClassObject(Shape shape, String name) {
    super(shape);
    this.name = name;
  }

  @Override
  public String toString() {
    return "<class " + name + ">";
  }

  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return toString();
  }

  @ExportMessage
  boolean hasMetaObject() { return true; }

  @ExportMessage
  Object getMetaObject() {
    return NimType.CLASS;
  }

  @ExportMessage
  boolean hasMembers() {
    return false;
  }
}
