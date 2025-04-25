package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.runtime.*;

import java.util.List;

public abstract class NRaiseStmtNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Executed
  @Child protected NNode error;

  public NRaiseStmtNode(NNode error) {
    this.error = error;
  }

  @Specialization(limit = "3")
  protected Object doValidError(NimObject value,
                                   @CachedLibrary("value") DynamicObjectLibrary messageLibrary,
                                   @CachedLibrary("value") DynamicObjectLibrary traceLibrary) {
    Object type = ((NimClass)value.classObject).name;
    Object message = messageLibrary.getOrDefault(value, "message", "");
    NimRuntimeError error = new NimRuntimeError(type, message, value, this);
    traceLibrary.putConstant(value, "stacktrace", formStackTrace(type, message, error), 0);
    throw error;
  }

  @Specialization
  protected Object doInvalidError(Object value) {
    throw NimRuntimeError.create(value, this);
  }

  @ExplodeLoop
  @CompilerDirectives.TruffleBoundary
  private TruffleString formStackTrace(Object type, Object message, NimRuntimeError easyScriptException) {
    TruffleStringBuilder sb = NString.builder();
    sb.appendJavaStringUTF16Uncached(String.valueOf(type));

    if (message != NimNil.SINGLETON) {
      sb.appendJavaStringUTF16Uncached(": ");
      sb.appendJavaStringUTF16Uncached(String.valueOf(message));
    }

    List<TruffleStackTraceElement> truffleStackTraceEls = TruffleStackTrace.getStackTrace(easyScriptException);
    for (TruffleStackTraceElement truffleStackTracEl : truffleStackTraceEls) {
      sb.appendJavaStringUTF16Uncached("\n\tat ");

      Node location = truffleStackTracEl.getLocation();
      RootNode rootNode = location.getRootNode();
      String funcName = rootNode.getName();

      SourceSection sourceSection = location.getEncapsulatingSourceSection();
      sb.appendJavaStringUTF16Uncached(sourceSection.getSource().getName());
      sb.appendJavaStringUTF16Uncached(":");
      sb.appendJavaStringUTF16Uncached(String.valueOf(sourceSection.getStartLine()));
      sb.appendJavaStringUTF16Uncached(":");
      sb.appendJavaStringUTF16Uncached(String.valueOf(sourceSection.getStartColumn()));

      sb.appendJavaStringUTF16Uncached(" -> ");

      // we want to ignore the top-level program RootNode type in this stack trace
      boolean isFunc = !":program".equals(funcName);
      if (isFunc) {
        sb.appendJavaStringUTF16Uncached(funcName);
      } else {
        sb.appendJavaStringUTF16Uncached("@.script");
      }

      sb.appendJavaStringUTF16Uncached("()");

      // TODO: Really consider if you want to show source for each stack trace
      /*String nearSection = location.getEncapsulatingSourceSection().getCharacters().toString();
      sb.appendJavaStringUTF16Uncached("\n\t\t\t" + nearSection.trim());*/
    }
    return sb.toStringUncached();
  }
}
