package org.blade.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.*;

import java.util.List;

public abstract class NRaiseStmtNode extends NStmtNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Executed
  @Child protected NNode error;

  private final boolean isAssert;

  public NRaiseStmtNode(NNode error, boolean isAssert) {
    this.error = error;
    this.isAssert = isAssert;
  }

  @Specialization(limit = "3")
  protected Object doValidError(BladeObject value,
                                @CachedLibrary("value") DynamicObjectLibrary messageLibrary,
                                @CachedLibrary("value") DynamicObjectLibrary traceLibrary) {
    Object type = ((BladeClass)value.classObject).name;
    Object message = messageLibrary.getOrDefault(value, "message", "");
    BladeRuntimeError error = BladeRuntimeError.create(type, message, value, this);
    traceLibrary.putConstant(value, "stacktrace", formStackTrace(type, message, error), 0);
    throw error;
  }

  @Specialization
  protected Object doStringError(TruffleString value) {
    if(isAssert) {
      throw BladeRuntimeError.assertError(this, value.toJavaStringUncached());
    }
    throw BladeRuntimeError.create(value, this);
  }

  @Specialization
  protected Object doOtherError(Object value) {
    if(isAssert) {
      throw BladeRuntimeError.assertError(this, BString.toString(value));
    }
    throw BladeRuntimeError.create(value, this);
  }

  @ExplodeLoop
  @CompilerDirectives.TruffleBoundary
  private TruffleString formStackTrace(Object type, Object message, BladeRuntimeError easyScriptException) {
    TruffleStringBuilder sb = BString.builder();
    sb.appendStringUncached(BString.fromObject(type));

    if (message != BladeNil.SINGLETON) {
      sb.appendStringUncached(BString.fromJavaString(": "));
      sb.appendStringUncached(BString.fromObject(message));
    }

    List<TruffleStackTraceElement> truffleStackTraceEls = TruffleStackTrace.getStackTrace(easyScriptException);
    for (TruffleStackTraceElement truffleStackTracEl : truffleStackTraceEls) {

      Node location = truffleStackTracEl.getLocation();
      SourceSection sourceSection = location.getEncapsulatingSourceSection();
      int startLine = sourceSection.getStartLine();

      if(startLine > -1) {
        RootNode rootNode = location.getRootNode();
        String funcName = rootNode.getName();

        sb.appendStringUncached(BString.fromJavaString("\n\tat "));

        sb.appendStringUncached(BString.fromJavaString(sourceSection.getSource().getName()));
        sb.appendStringUncached(BString.fromJavaString(":"));
        sb.appendStringUncached(BString.fromObject(startLine));
        sb.appendStringUncached(BString.fromJavaString(":"));
        sb.appendStringUncached(BString.fromObject(sourceSection.getStartColumn()));

        sb.appendStringUncached(BString.fromJavaString(" -> "));

        // we want to ignore the top-level program RootNode type in this stack trace
        boolean isFunc = !":program".equals(funcName);
        if (isFunc) {
          sb.appendStringUncached(BString.fromJavaString(funcName));
        } else {
          sb.appendStringUncached(BString.fromJavaString("@.script"));
        }

        sb.appendStringUncached(BString.fromJavaString("()"));

        // TODO: Really consider if you want to show source for each stack trace
        /*String nearSection = location.getEncapsulatingSourceSection().getCharacters().toString();
        sb.appendStringUncached(BString.fromJavaString("\n\t\t\t" + nearSection.trim()));*/
      }
    }
    return sb.toStringUncached();
  }
}
