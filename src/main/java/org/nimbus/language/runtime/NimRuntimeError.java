package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.nimbus.annotations.NAnnotationHelper;

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
      result.append(sep);
      sep = ", ";

      if(o instanceof NClassInstance classInstance) {
        result.append(classInstance.getClassName());
      } else {String[] qualifiedName = NAnnotationHelper.getObjectName(o.getClass()).split("[.]");
        String name = qualifiedName[qualifiedName.length - 1];
        if(name.equals("TruffleString")) {
          name = "String";
        }

        result.append(name);
      }

    }
    result.append(")");

    return NimRuntimeError.create(result.toString(), node);
  }
}
