package org.zuri.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import org.zuri.language.nodes.NNode;
import org.zuri.language.shared.ZuriUtil;

public class ZuriRuntimeError extends AbstractTruffleException {
  public final Object value;

  public ZuriRuntimeError(Object value) {
    this.value = value;
  }

  public ZuriRuntimeError(String message, Node node) {
    super(message, node);
    this.value = null;
  }

  public ZuriRuntimeError(Object value, Node node) {
    super(ZString.toString(value), node);
    this.value = value;
  }

  public ZuriRuntimeError(ErrorObject value, Node node) {
    super(value.type + ": " + value.message, node);
    this.value = value;
  }

  public ZuriRuntimeError(String message) {
    this(message, null);
  }

//  private static final InteropLibrary UNCACHED_LIB = InteropLibrary.getFactory().getUncached();

  public ZuriRuntimeError(Object name, Object message, ZuriObject value, NNode node) {
    super(ZString.toString(name) + ": " + ZString.toString(message), node);
    this.value = value;
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(Object value, Node location) {
    return new ZuriRuntimeError(value, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(ErrorObject value, Node location) {
    return new ZuriRuntimeError(value, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message, Node location) {
    return new ZuriRuntimeError(message, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message) {
    return new ZuriRuntimeError(message);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message, Object... others) {
    return new ZuriRuntimeError(ZString.concatString(message, others));
  }

  public static AbstractTruffleException error(Node node, String message, Object... values) {
    return create(ErrorObject.create(node, "Error", ZString.concatString(message, values)), node);
  }

  public static AbstractTruffleException typeError(Node node, String message, Object... values) {
    return create(ErrorObject.create(node, "TypeError", ZString.concatString(message, values)), node);
  }

  public static AbstractTruffleException valueError(Node node, String message) {
    return create(ErrorObject.create(node, "ValueError", message), node);
  }

  public static AbstractTruffleException valueError(Node node, String message, Object... values) {
    return create(ErrorObject.create(node, "ValueError", ZString.concatString(message, values)), node);
  }

  public static AbstractTruffleException assertError(Node node, String message) {
    return create(ErrorObject.create(node, "AssertError", message), node);
  }

  public static AbstractTruffleException assertError(Node node, String message, Object... values) {
    return create(ErrorObject.create(node, "AssertError", ZString.concatString(message, values)), node);
  }

  @ExplodeLoop
  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException argumentError(Node node, String operation, Object... values) {
    StringBuilder result = new StringBuilder();

    if (operation != null) {
      result.append(operation);
    } else {
      result.append("operation");
    }

    result.append(" is not defined for call signature");

    String sep = " (";
    for (Object o : values) {
      result.append(sep);
      sep = ", ";

      if (o instanceof ZuriObject classInstance) {
        result.append(classInstance.getClassName());
      } else {
        result.append(ZuriUtil.getObjectType(o));
      }

    }
    result.append(")");

    return create(ErrorObject.create(node, "ArgumentError", result.toString()), node);
  }

  @CompilerDirectives.TruffleBoundary
  public static ZuriRuntimeError create(Object name, Object message, ZuriObject value, NNode node) {
    return new ZuriRuntimeError(name, message, value, node);
  }
}
