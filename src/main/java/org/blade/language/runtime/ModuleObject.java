package org.blade.language.runtime;

import com.oracle.truffle.api.object.Shape;

//@ExportLibrary(InteropLibrary.class)
public final class ModuleObject extends GlobalScopeObject {
  public ModuleObject(Shape shape) {
    super(shape);
  }
}
