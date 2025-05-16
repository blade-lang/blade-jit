package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

public final class ErrorObject extends BladeObject {
  public final String type, message;
  private static final DynamicObjectLibrary UNCACHED_OBJ = DynamicObjectLibrary.getUncached();

  public ErrorObject(String message, DynamicObjectLibrary objectLibrary, Shape shape, BladeClass prototype) {
    super(shape, prototype);

    this.type = prototype.name;
    this.message = message;
    objectLibrary.put(this, "type", prototype.name);
    objectLibrary.put(this, "message", BString.fromJavaString(message));
  }

  public ErrorObject(String message, Shape shape, BladeClass prototype) {
    this(message, UNCACHED_OBJ, shape, prototype);
  }

  @CompilerDirectives.TruffleBoundary
  public static ErrorObject create(Node node, String type, String message) {
    BladeContext context = BladeContext.get(node);

    return new ErrorObject(
      message,
      context.objectsModel.rootShape,
      switch (type) {
        case "ArgumentError" -> context.objectsModel.errorsModel.argumentError;
        case "TypeError" -> context.objectsModel.errorsModel.typeError;
        case "ValueError" -> context.objectsModel.errorsModel.valueError;
        case "AssertError" -> context.objectsModel.errorsModel.assertError;
        default -> context.objectsModel.errorsModel.error;
      }
    );
  }
}
