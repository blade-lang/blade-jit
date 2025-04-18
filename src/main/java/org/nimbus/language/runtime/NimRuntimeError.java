package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

public class NimRuntimeError extends AbstractTruffleException {
  public NimRuntimeError(String message) {
    super(message);
  }

  private static final InteropLibrary UNCACHED_LIB = InteropLibrary.getFactory().getUncached();

  NimRuntimeError(String message, Node node) {
    super(message, node);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException create(String message, Node location) {
    return new NimRuntimeError(message, location);
  }

  @CompilerDirectives.TruffleBoundary
  public static AbstractTruffleException argumentError(Node node, String operationName, Object... values) {
    StringBuilder result = new StringBuilder();
    result.append("Type error");

    AbstractTruffleException ex = NimRuntimeError.create("", node);
    if (node != null) {
      SourceSection ss = ex.getEncapsulatingSourceSection();
      if (ss != null && ss.isAvailable()) {
        result.append(" at ").append(ss.getSource().getName()).append(" line ").append(ss.getStartLine()).append(" col ").append(ss.getStartColumn());
      }
    }

    result.append(": ");
    if (node != null) {
      result.append(" \"").append(operationName).append("\"");
    }

    result.append(" is not defined for call signature");

    String sep = " (";
    for (Object o : values) {
      Object value = NimLanguageView.forValue(o);
      result.append(sep);
      sep = ", ";
      if (value == null) {
        result.append("unknown");
      } else {
        InteropLibrary valueLib = InteropLibrary.getFactory().getUncached(value);
        if (valueLib.hasMetaObject(value) && !valueLib.isNull(value)) {
          String qualifiedName;
          try {
            qualifiedName = UNCACHED_LIB.asString(UNCACHED_LIB.getMetaQualifiedName(valueLib.getMetaObject(value)));
          } catch (UnsupportedMessageException e) {
            throw shouldNotReachHere(e);
          }
          result.append(qualifiedName);
          result.append(" ");
        }
      }
    }
    result.append(")");

    return NimRuntimeError.create(result.toString(), node);
  }
}
