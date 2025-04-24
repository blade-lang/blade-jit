package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

public final class NErrorObject extends NimObject {
  public final String type, message;

//  private static final InteropLibrary UNCACHED_LIB = InteropLibrary.getFactory().getUncached();
  private static final DynamicObjectLibrary UNCACHED_OBJ = DynamicObjectLibrary.getUncached();

  public NErrorObject(
    String type, String message, DynamicObjectLibrary objectLibrary,
    Shape shape, NimClass prototype
  ) {
    super(shape, prototype);

    this.type = type;
    this.message = message;
    objectLibrary.put(this, "type", NString.fromJavaString(type));
    objectLibrary.put(this, "message", NString.fromJavaString(message));
  }

  @CompilerDirectives.TruffleBoundary
  public static NErrorObject create(Node node, String type, String message) {
    NimContext context = NimContext.get(node);

    return new NErrorObject(
      type,
      message,
      UNCACHED_OBJ,
      context.objectsModel.rootShape,
      switch (type) {
        case "ArgumentError" -> context.objectsModel.errorsModel.argumentError;
        case "TypeError" -> context.objectsModel.errorsModel.typeError;
        case "ValueError" -> context.objectsModel.errorsModel.valueError;
        default -> context.objectsModel.errorsModel.error;
      }
    );
  }
}
