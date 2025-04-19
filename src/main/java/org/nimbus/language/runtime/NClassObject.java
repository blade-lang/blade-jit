package org.nimbus.language.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.annotations.ObjectName;
import org.nimbus.language.NimbusLanguage;

@ExportLibrary(InteropLibrary.class)
@ObjectName("Class")
public class NClassObject extends DynamicObject {
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
}
