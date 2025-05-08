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
  protected int doInts(int left, int right) {
    return Math.addExact(left, right);
  }

  @Specialization(guards = {"isDouble(left)", "isInt(right)"})
  protected double doDoubleInt(double left, int right) {
    return left + right;
  }

  @Specialization(guards = {"isInt(left)", "isDouble(right)"})
  protected double doIntDouble(int left, double right) {
    return left + right;
  }

  @Specialization(replaces = "doInts")
  protected double doDoubles(double left, double right) {
    return left + right;
  }

  @Specialization
  protected TruffleString doStrings(TruffleString left, TruffleString right,
                                    @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return NString.concat(concatNode, left, right);
  }

  @Specialization
  protected TruffleString doStringInt(TruffleString left, int right,
                                    @Cached @Cached.Shared("fromLongNode") TruffleString.FromLongNode fromLongNode,
                                    @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return NString.concat(concatNode, left, NString.fromInt(fromLongNode, right));
  }

  @Specialization
  protected TruffleString doIntString(int left, TruffleString right,
                                    @Cached @Cached.Shared("fromLongNode") TruffleString.FromLongNode fromLongNode,
                                    @Cached @Cached.Shared("concatNode") TruffleString.ConcatNode concatNode) {
    return NString.concat(concatNode, NString.fromInt(fromLongNode, left), right);
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
    throw NimRuntimeError.argumentError(this,"+", left, right);
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
