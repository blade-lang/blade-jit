package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NimRuntimeError;

public abstract class NAddNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLongs(long left, long right) {
    return Math.addExact(left, right);
  }

  @Specialization(replaces = "doLongs")
  protected double doDoubles(double left, double right) {
    return left + right;
  }

  @Specialization
  protected TruffleString doStrings(TruffleString left, TruffleString right,
                                    @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return concatNode.execute(left, right, NimbusLanguage.ENCODING, true);
  }

  @CompilerDirectives.TruffleBoundary
  @Specialization(guards = "isString(left, right)")
  protected TruffleString doStringConverted(Object left, Object right,
                                            @Cached TruffleString.FromJavaStringNode leftFromJavaNode,
                                            @Cached TruffleString.FromJavaStringNode rightFromJavaNode,
                                            @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return concatNode.execute(
      leftFromJavaNode.execute(left.toString(), NimbusLanguage.ENCODING),
      rightFromJavaNode.execute(right.toString(), NimbusLanguage.ENCODING),
      NimbusLanguage.ENCODING, true
    );
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw new NimRuntimeError("operation + is undefined for object of types");
  }

  protected static boolean isString(Object left, Object right) {
    return left instanceof TruffleString || right instanceof TruffleString;
  }

  /*private static boolean isPrimitive(Object value) {
    return RTypesGen.isImplicitDouble(value) ||
      RTypesGen.isBoolean(value) ||
      value == RemNil.SINGLETON;
  }*/
}
