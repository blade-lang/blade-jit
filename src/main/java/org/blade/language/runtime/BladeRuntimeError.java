package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import org.blade.language.nodes.NNode;
import org.blade.language.shared.BladeUtil;

public class BladeRuntimeError extends AbstractTruffleException {
  public final Object value;

  public BladeRuntimeError(Object value) {
    this.value = value;
  }

  public BladeRuntimeError(String message, Node node) {
    super(message, node);
    this.value = null;
  }

  public BladeRuntimeError(Object value, Node node) {
    super(BString.toString(value), node);
    this.value = value;
  }

  public BladeRuntimeError(ErrorObject value, Node node) {
    super(value.type + ": " + value.message, node);
    this.value = value;
  }

  public BladeRuntimeError(String message) {
    this(message, null);
  }

//  private static final InteropLibrary UNCACHED_LIB = InteropLibrary.getFactory().getUncached();

  public BladeRuntimeError(Object name, Object message, BladeObject value, NNode node) {
    super(BString.toString(name) + ": " + BString.toString(message), node);
    this.value = value;
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(Object value, Node location) {
    return new BladeRuntimeError(value, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(ErrorObject value, Node location) {
    return new BladeRuntimeError(value, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message, Node location) {
    return new BladeRuntimeError(message, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message) {
    return new BladeRuntimeError(message);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message, Object... others) {
    return new BladeRuntimeError(BString.concatString(message, others));
  }

  public static AbstractTruffleException error(Node node, String message, Object... values) {
    return create(ErrorObject.create(node, "Error", BString.concatString(message, values)), node);
  }

  public static AbstractTruffleException typeError(Node node, String message, Object... values) {
    return create(ErrorObject.create(node, "TypeError", BString.concatString(message, values)), node);
  }

  public static AbstractTruffleException valueError(Node node, String message) {
    return create(ErrorObject.create(node, "ValueError", message), node);
  }

  public static AbstractTruffleException valueError(Node node, String message, Object... values) {
    return create(ErrorObject.create(node, "ValueError", BString.concatString(message, values)), node);
  }

  public static AbstractTruffleException assertError(Node node, String message) {
    return create(ErrorObject.create(node, "AssertError", message), node);
  }

  public static AbstractTruffleException assertError(Node node, String message, Object... values) {
    return create(ErrorObject.create(node, "AssertError", BString.concatString(message, values)), node);
  }

  @ExplodeLoop
  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException argumentError(Node node, String operation, Object... values) {
    StringBuilder result = new StringBuilder();

    if (operation != null) {
      result.append("'").append(operation).append("'");
    } else {
      result.append("operation");
    }

    result.append(" is not defined for call signature");

    String sep = " (";
    for (Object o : values) {
      result.append(sep);
      sep = ", ";

      if (o instanceof BladeObject classInstance) {
        result.append(classInstance.getClassName());
      } else {
        result.append(BladeUtil.getObjectType(o));
      }

    }
    result.append(")");

    return create(ErrorObject.create(node, "ArgumentError", result.toString()), node);
  }

  @CompilerDirectives.TruffleBoundary
  public static BladeRuntimeError create(Object name, Object message, BladeObject value, NNode node) {
    return new BladeRuntimeError(name, message, value, node);
  }
}
