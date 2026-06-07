package org.zuri.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

public final class ErrorObject extends ZuriObject {
  private static final DynamicObjectLibrary UNCACHED_OBJ = DynamicObjectLibrary.getUncached();
  public final String type, message;

  public ErrorObject(String message, Shape shape, ZuriClass prototype) {
    super(shape, prototype);

    this.type = prototype.name;
    this.message = message;
    UNCACHED_OBJ.put(this, "type", prototype.name);
    UNCACHED_OBJ.put(this, "message", ZString.fromJavaString(message));
  }

  @CompilerDirectives.TruffleBoundary
  public static ErrorObject create(Node node, String type, String message) {
    ZuriContext context = ZuriContext.get(node);

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
