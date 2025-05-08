package org.nimbus.language.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NBaseBuiltinDeclaration;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.runtime.*;
import org.nimbus.utility.RegulatedMap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class NBuiltinFunctions implements NBaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("time", false, NBuiltinFunctionsFactory.TimeFunctionNodeFactory.getInstance());
      add("print", true, NBuiltinFunctionsFactory.PrintFunctionNodeFactory.getInstance());
      add("microtime", false, NBuiltinFunctionsFactory.MicroTimeFunctionNodeFactory.getInstance());
      add("abs", false, NBuiltinFunctionsFactory.AbsFunctionNodeFactory.getInstance());
      add("bin", false, NBuiltinFunctionsFactory.BinFunctionNodeFactory.getInstance());
      add("chr", false, NBuiltinFunctionsFactory.ChrFunctionNodeFactory.getInstance());
      add("hex", false, NBuiltinFunctionsFactory.HexFunctionNodeFactory.getInstance());
      add("id", false, NBuiltinFunctionsFactory.IdFunctionNodeFactory.getInstance());
    }};
  }

  public abstract static class TimeFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected int doAny() {
      return time() / 1000;
    }

    @CompilerDirectives.TruffleBoundary
    private int time() {
      return (int)System.currentTimeMillis();
    }
  }

  public abstract static class PrintFunctionNode extends NBuiltinFunctionNode {

    @Specialization
    public Object doList(NListObject object,
                         @CachedLibrary(limit = "3") InteropLibrary interopLibrary,
                         @Cached(value = "languageContext()", neverDefault = false) NimContext context) {
      print(context, interopLibrary, object.items);
      return NimNil.SINGLETON;
    }

    @Fallback
    protected Object fallback(Object object) {
      NimContext.get(this).println(NString.concatString("Something not working right: ", object));
      return NimNil.SINGLETON;
    }

    @ExplodeLoop
    private void print(NimContext context, InteropLibrary interopLibrary, Object[] arguments) {
      int length = arguments.length;

      for (int i = 0; i < length - 1; i++) {
        if (arguments[i] != NimNil.SINGLETON) {
          context.print(NString.fromObject(interopLibrary, arguments[i]));
          context.print(" ");
        }
      }

      if (arguments[length - 1] != NimNil.SINGLETON) {
        context.print(NString.fromObject(interopLibrary, arguments[length - 1]));
      }

      context.flushOutput();
    }
  }

  public abstract static class MicroTimeFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected double doAny() {
      return microTime();
    }

    @CompilerDirectives.TruffleBoundary
    private double microTime() {
      return ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
    }
  }

  public abstract static class AbsFunctionNode extends NBuiltinFunctionNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected int doInt(int arg) {
      return arg < 0 ? Math.negateExact(arg) : arg;
    }

    @Specialization(replaces = "doInt")
    protected double doDouble(double arg) {
      return Math.abs(arg);
    }

    @Fallback
    protected double notANumber(Object object) {
      return Double.NaN;
    }
  }

  public abstract static class BinFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doInt(int arg,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      return NString.fromObject(fromJavaStringNode, Integer.toBinaryString(arg));
    }

    @Fallback
    protected Object doInvalid(Object object) {
      throw NimRuntimeError.argumentError(this, "bin", object);
    }
  }

  public abstract static class ChrFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doInt(int arg,
                                   @Cached TruffleString.FromCodePointNode fromCodePointNode,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      return NString.fromObject(fromJavaStringNode, NString.fromCodePoint(fromCodePointNode, arg));
    }

    @Fallback
    protected Object doInvalid(Object object) {
      throw NimRuntimeError.argumentError(this, "chr", object);
    }
  }

  public abstract static class HexFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doInt(int arg,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      return NString.fromObject(fromJavaStringNode, Integer.toHexString(arg));
    }

    @Fallback
    protected double doInvalid(Object object) {
      throw NimRuntimeError.argumentError(this, "hex", object);
    }
  }

  public abstract static class IdFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected int doNimObject(NimObject arg) {
      return arg.hash();
    }

    @Fallback
    protected int doOthers(Object object) {
      return hash(object);
    }

    @CompilerDirectives.TruffleBoundary
    protected int hash(Object object) {
      return object.hashCode();
    }
  }
}
