package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NString;
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
    return NString.concat(concatNode, left, right);
  }

  @Specialization
  protected TruffleString doStringLong(TruffleString left, long right,
                                    @Cached @Cached.Shared("fromLongNode") TruffleString.FromLongNode fromLongNode,
                                    @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return NString.concat(concatNode, left, NString.fromLong(fromLongNode, right));
  }

  @Specialization
  protected TruffleString doLongString(long left, TruffleString right,
                                    @Cached @Cached.Shared("fromLongNode") TruffleString.FromLongNode fromLongNode,
                                    @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return NString.concat(concatNode, NString.fromLong(fromLongNode, left), right);
  }

  @CompilerDirectives.TruffleBoundary
  @Specialization(guards = "isString(left, right)")
  protected TruffleString doStringConverted(Object left, Object right,
                                            @Cached TruffleString.FromJavaStringNode leftFromJavaNode,
                                            @Cached TruffleString.FromJavaStringNode rightFromJavaNode,
                                            @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return NString.concat(
      concatNode,
      NString.fromObject(leftFromJavaNode, left),
      NString.fromObject(rightFromJavaNode, right)
    );
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this,"operation +", left, right);
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
