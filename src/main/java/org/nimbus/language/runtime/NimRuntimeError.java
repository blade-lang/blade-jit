package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;
import org.nimbus.annotations.NAnnotationHelper;
import org.nimbus.language.nodes.NNode;

public class NimRuntimeError extends AbstractTruffleException {
  public final Object value;

  public NimRuntimeError(Object value) {
    this.value = value;
  }

  public NimRuntimeError(String message, Node node) {
    super(message, node);
    this.value = null;
  }

  public NimRuntimeError(Object value, Node node) {
    super(NString.toString(value), node);
    this.value = value;
  }

  public NimRuntimeError(NErrorObject value, Node node) {
    super(value.type + ": " + value.message, node);
    this.value = value;
  }

  public NimRuntimeError(String message) {
    this(message, null);
  }

//  private static final InteropLibrary UNCACHED_LIB = InteropLibrary.getFactory().getUncached();

  public NimRuntimeError(Object name, Object message, NimObject value, NNode node) {
    super(NString.toString(name) + ": " + NString.toString(message), node);
    this.value = value;
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(Object value, Node location) {
    return new NimRuntimeError(value, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(NErrorObject value, Node location) {
    return new NimRuntimeError(value, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message, Node location) {
    return new NimRuntimeError(message, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message) {
    return new NimRuntimeError(message);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message, Object ...others) {
    return new NimRuntimeError(NString.concatString(message, others));
  }

  public static AbstractTruffleException error(Node node, String message, Object ...values) {
    return create(NErrorObject.create(node, "Error", NString.concatString(message, values)), node);
  }

  public static AbstractTruffleException typeError(Node node, String message, Object ...values) {
    return create(NErrorObject.create(node, "TypeError", NString.concatString(message, values)), node);
  }

  public static AbstractTruffleException valueError(Node node, String message) {
    return create(NErrorObject.create(node, "ValueError", message), node);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException argumentError(Node node, String operation, Object ...values) {
    StringBuilder result = new StringBuilder();

    if (operation != null) {
      result.append("\"").append(operation).append("\"");
    } else {
      result.append("operation");
    }

    result.append(" is not defined for call signature");

    String sep = " (";
    for (Object o : values) {
      result.append(sep);
      sep = ", ";

      if(o instanceof NimObject classInstance) {
        result.append(classInstance.getClassName());
      } else {
        String[] qualifiedName = NAnnotationHelper.getObjectName(o.getClass()).split("[.]");
        String name = qualifiedName[qualifiedName.length - 1];
        if(name.equals("TruffleString")) {
          name = "String";
        }

        result.append(name);
      }

    }
    result.append(")");

    return create(NErrorObject.create(node, "ArgumentError", result.toString()), node);
  }
}
