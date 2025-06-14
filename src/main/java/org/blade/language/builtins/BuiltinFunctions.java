package org.blade.language.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BaseBuiltinDeclaration;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.runtime.*;
import org.blade.language.shared.BuiltinClassesModel;
import org.blade.utility.RegulatedMap;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public final class BuiltinFunctions implements BaseBuiltinDeclaration {
  @Override
  public RegulatedMap<String, Boolean, NodeFactory<? extends NBuiltinFunctionNode>> getDeclarations() {
    return new RegulatedMap<>() {{
      add("time", false, BuiltinFunctionsFactory.TimeFunctionNodeFactory.getInstance());
      add("print", true, BuiltinFunctionsFactory.PrintFunctionNodeFactory.getInstance());
      add("microtime", false, BuiltinFunctionsFactory.MicroTimeFunctionNodeFactory.getInstance());
      add("abs", false, BuiltinFunctionsFactory.AbsFunctionNodeFactory.getInstance());
      add("bin", false, BuiltinFunctionsFactory.BinFunctionNodeFactory.getInstance());
      add("chr", false, BuiltinFunctionsFactory.ChrFunctionNodeFactory.getInstance());
      add("hex", false, BuiltinFunctionsFactory.HexFunctionNodeFactory.getInstance());
      add("id", false, BuiltinFunctionsFactory.IdFunctionNodeFactory.getInstance());
      add("instance_of", false, BuiltinFunctionsFactory.InstanceOfMethodNodeFactory.getInstance());
      add("max", false, BuiltinFunctionsFactory.MaxFunctionNodeFactory.getInstance());
      add("min", false, BuiltinFunctionsFactory.MinFunctionNodeFactory.getInstance());
      add("oct", false, BuiltinFunctionsFactory.OctFunctionNodeFactory.getInstance());
      add("ord", false, BuiltinFunctionsFactory.OrdFunctionNodeFactory.getInstance());
      add("rand", false, BuiltinFunctionsFactory.RandFunctionNodeFactory.getInstance());
      add("to_number", false, BuiltinFunctionsFactory.ToNumberFunctionNodeFactory.getInstance());
    }};
  }

  public abstract static class TimeFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected long doAny() {
      return time() / 1000;
    }

    @CompilerDirectives.TruffleBoundary
    private long time() {
      return System.currentTimeMillis();
    }
  }

  public abstract static class PrintFunctionNode extends NBuiltinFunctionNode {

    @Specialization
    public Object doList(ListObject object,
                         @CachedLibrary(limit = "3") InteropLibrary interopLibrary,
                         @Cached(value = "languageContext()", neverDefault = false) BladeContext context) {
      print(context, interopLibrary, object.items);
      return BladeNil.SINGLETON;
    }

    @Fallback
    protected Object fallback(Object object) {
      BladeContext.get(this).println(BString.concatString("Something not working right: ", object));
      return BladeNil.SINGLETON;
    }

    @ExplodeLoop
    private void print(BladeContext context, InteropLibrary interopLibrary, Object[] arguments) {
      int length = arguments.length;

      if (length > 0) {
        for (int i = 0; i < length - 1; i++) {
          if (arguments[i] != BladeNil.SINGLETON) {
            context.print(BString.fromObject(interopLibrary, arguments[i]));
            context.print(" ");
          }
        }

        if (arguments[length - 1] != BladeNil.SINGLETON) {
          context.print(BString.fromObject(interopLibrary, arguments[length - 1]));
        }

        context.flushOutput();
      }
    }
  }

  public abstract static class MicroTimeFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected long doAny() {
      return microTime();
    }

    @CompilerDirectives.TruffleBoundary
    private long microTime() {
      return ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
    }
  }

  public abstract static class AbsFunctionNode extends NBuiltinFunctionNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected long doLong(long arg) {
      return arg < 0 ? Math.negateExact(arg) : arg;
    }

    @Specialization(replaces = "doLong")
    protected double doDouble(double arg) {
      return Math.abs(arg);
    }

    @Specialization
    protected Object doObject(BladeObject arg, @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      return methodOverride(this, "@abs", arg, interopLibrary, Double.NaN);
    }

    @Fallback
    protected double notANumber(Object object) {
      return Double.NaN;
    }
  }

  public abstract static class BinFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doLong(long arg,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      return BString.fromObject(fromJavaStringNode, Long.toBinaryString(arg));
    }

    @Fallback
    protected double doInvalid(Object object) {
      throw BladeRuntimeError.argumentError(this, "bin", object);
    }
  }

  public abstract static class ChrFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doLong(long arg,
                                   @Cached TruffleString.FromCodePointNode fromCodePointNode,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if (arg >= 0x110000) {
        throw BladeRuntimeError.valueError(this, "chr() argument out of maximum UTF-16 character range 0x10FFFE");
      }
      return BString.fromObject(fromJavaStringNode, BString.fromCodePoint(fromCodePointNode, (int) arg));
    }

    @Fallback
    protected double doInvalid(Object object) {
      throw BladeRuntimeError.argumentError(this, "chr", object);
    }
  }

  public abstract static class HexFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doLong(long arg,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      return BString.fromObject(fromJavaStringNode, Long.toHexString(arg));
    }

    @Fallback
    protected double doInvalid(Object object) {
      throw BladeRuntimeError.argumentError(this, "hex", object);
    }
  }

  public abstract static class IdFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected long doNimObject(BladeObject arg) {
      return arg.hash();
    }

    @Fallback
    protected long doOthers(Object object) {
      return hash(object);
    }

    @CompilerDirectives.TruffleBoundary
    protected long hash(Object object) {
      return object.hashCode();
    }
  }

  public abstract static class InstanceOfMethodNode extends NBuiltinFunctionNode {

    @ExplodeLoop
    @Specialization
    protected boolean doObject(BladeObject object, BladeClass testClass,
                               @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                               @Cached(value = "objectsModel.objectObject", neverDefault = true) BladeClass objectObject) {
      BladeObject klassObject = (BladeObject) object.classObject;
      if (klassObject == testClass) return true;

      while (klassObject != null && klassObject != objectObject) {
        if (klassObject.classObject == testClass) return true;
        klassObject = (BladeObject) klassObject.classObject;
      }

      return false;
    }

    @Specialization
    protected boolean doString(TruffleString string, BladeClass testClass,
                               @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                               @Cached(value = "objectsModel.objectObject", neverDefault = true) BladeClass objectObject) {
      return testClass == objectsModel.stringObject || testClass == objectObject;
    }

    @Specialization
    protected boolean doLong(long value, BladeClass testClass,
                             @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                             @Cached(value = "objectsModel.objectObject", neverDefault = true) BladeClass objectObject) {
      return testClass == objectsModel.numberObject || testClass == objectObject;
    }

    @Specialization
    protected boolean doDouble(double value, BladeClass testClass,
                               @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                               @Cached(value = "objectsModel.objectObject", neverDefault = true) BladeClass objectObject) {
      return testClass == objectsModel.numberObject || testClass == objectObject;
    }

    @Specialization
    protected boolean doBoolean(boolean value, BladeClass testClass,
                                @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                                @Cached(value = "objectsModel.objectObject", neverDefault = true) BladeClass objectObject) {
      return testClass == objectsModel.booleanObject || testClass == objectObject;
    }

    @Fallback
    protected boolean doOthers(Object object, Object klass) {
      return false;
    }
  }

  public abstract static class MaxFunctionNode extends NBuiltinFunctionNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected long doLongs(long left, long right) {
      return Math.max(left, right);
    }

    @Specialization(guards = {"isDouble(left)", "isLong(right)"})
    protected double doDoubleLong(double left, long right) {
      return Math.max(left, right);
    }

    @Specialization(guards = {"isLong(left)", "isDouble(right)"})
    protected double doLongDouble(long left, double right) {
      return Math.max(left, right);
    }

    @Specialization(replaces = "doLongs")
    protected double doDoubles(double left, double right) {
      return Math.max(left, right);
    }

    @Specialization
    protected TruffleString doStrings(TruffleString left, TruffleString right,
                                      @Cached TruffleString.CompareBytesNode compareNode) {
      return compareNode.execute(left, right, BladeLanguage.ENCODING) > 0 ? left : right;
    }

    @Fallback
    protected double doInvalid(Object left, Object right) {
      throw BladeRuntimeError.argumentError(this, "max", left, right);
    }
  }

  public abstract static class MinFunctionNode extends NBuiltinFunctionNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected long doLongs(long left, long right) {
      return Math.min(left, right);
    }

    @Specialization(guards = {"isDouble(left)", "isLong(right)"})
    protected double doDoubleLong(double left, long right) {
      return Math.min(left, right);
    }

    @Specialization(guards = {"isLong(left)", "isDouble(right)"})
    protected double doLongDouble(long left, double right) {
      return Math.min(left, right);
    }

    @Specialization(replaces = "doLongs")
    protected double doDoubles(double left, double right) {
      return Math.min(left, right);
    }

    @Specialization
    protected TruffleString doStrings(TruffleString left, TruffleString right,
                                      @Cached TruffleString.CompareBytesNode compareNode) {
      return compareNode.execute(left, right, BladeLanguage.ENCODING) > 0 ? right : left;
    }

    @Fallback
    protected double doInvalid(Object left, Object right) {
      throw BladeRuntimeError.argumentError(this, "max", left, right);
    }
  }

  public abstract static class OctFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doLong(long arg,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      return BString.fromObject(fromJavaStringNode, Long.toString(arg, 8));
    }

    @Fallback
    protected double doInvalid(Object object) {
      throw BladeRuntimeError.argumentError(this, "oct", object);
    }
  }

  public abstract static class OrdFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected long doLong(TruffleString string,
                          @Cached TruffleString.CodePointAtIndexNode codePointNode,
                          @Cached TruffleString.CodePointLengthNode lengthNode) {
      long stringLength = BString.length(string, lengthNode);
      if (stringLength != 1) {
        throw BladeRuntimeError.valueError(
          this,
          "ord() expected a character, but string of length ",
          stringLength,
          " given"
        );
      }
      return BString.toCodePoint(string, codePointNode, 0);
    }

    @Fallback
    protected double doInvalid(Object object) {
      throw BladeRuntimeError.argumentError(this, "ord", object);
    }
  }

  public abstract static class RandFunctionNode extends NBuiltinFunctionNode {
    private SecureRandom secureRandom = null;

    @CompilerDirectives.TruffleBoundary
    @Specialization
    protected double doNilNilNil(BladeNil min, BladeNil max, BladeNil secure) {
      return ThreadLocalRandom.current().nextDouble();
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "secure == true")
    protected double doTrueNilNil(boolean secure, BladeNil min, BladeNil max) {
      return getSecureRandom().nextDouble();
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "secure == false")
    protected double doFalseNilNil(boolean secure, BladeNil min, BladeNil max) {
      return ThreadLocalRandom.current().nextDouble();
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization
    protected long doLongNilNil(long max, BladeNil ignored, BladeNil secure) {
      return ThreadLocalRandom.current().nextLong(max);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "min < max")
    protected long doLongLongNil(long min, long max, BladeNil secure) {
      return ThreadLocalRandom.current().nextLong(min, max);
    }

    @Specialization(guards = "max <= min")
    protected Object doLongLongNilInvalid(long min, long max, BladeNil secure) {
      return invalidOrder(min, max, secure);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = {"secure == true", "min < max"})
    protected long doLongLongTrue(long min, long max, boolean secure) {
      return getSecureRandom().nextLong(min, max);
    }

    @Specialization(guards = {"secure == true", "max >= min"})
    protected Object doLongLongTrueInvalid(long min, long max, boolean secure) {
      return invalidOrder(min, max, secure);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = {"secure == false", "min < max"})
    protected long doLongLongFalse(long min, long max, boolean secure) {
      return ThreadLocalRandom.current().nextLong(min, max);
    }

    @Specialization(guards = {"secure == false", "max <= min"})
    protected Object doLongLongFalseInvalid(long min, long max, boolean secure) {
      return invalidOrder(min, max, secure);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization
    protected double doDoubleNilNil(double max, BladeNil ignored, BladeNil secure) {
      return ThreadLocalRandom.current().nextDouble(max);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "min < max")
    protected double doDoubleDoubleNil(double min, double max, BladeNil secure) {
      return ThreadLocalRandom.current().nextDouble(min, max);
    }

    @Specialization(guards = "max <= min")
    protected Object doDoubleDoubleNilInvalid(double min, double max, BladeNil secure) {
      return invalidOrder(min, max, secure);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = {"secure == true", "min < max"})
    protected double doDoubleDoubleTrue(double min, double max, boolean secure) {
      return getSecureRandom().nextDouble(min, max);
    }

    @Specialization(guards = {"secure == true", "max <= min"})
    protected Object doDoubleDoubleTrueInvalid(double min, double max, boolean secure) {
      return invalidOrder(min, max, true);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = {"secure == false", "min < max"})
    protected double doDoubleDoubleFalse(double min, double max, boolean secure) {
      return ThreadLocalRandom.current().nextDouble(min, max);
    }

    @Specialization(guards = {"secure == false", "max <= min"})
    protected Object doDoubleDoubleFalseInvalid(double min, double max, boolean secure) {
      return invalidOrder(min, max, secure);
    }

    private Object invalidOrder(Object min, Object max, Object secure) {
      throw BladeRuntimeError.valueError(this, "ranged rand() requires that min value < max boundary");
    }

    @Fallback
    protected double invalid(Object min, Object max, Object secure) {
      throw BladeRuntimeError.argumentError(this, "rand()", min, max, secure);
    }

    @CompilerDirectives.TruffleBoundary
    private SecureRandom getSecureRandom() {
      if (secureRandom == null) {
        try {
          secureRandom = SecureRandom.getInstanceStrong(); // Get the strongest available algorithm
        } catch (NoSuchAlgorithmException e) {
          secureRandom = new SecureRandom(); // Fallback to default SecureRandom
        }
      }
      return secureRandom;
    }
  }

  @ImportStatic(BString.class)
  public abstract static class ToNumberFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected long doLong(long value) {
      return value;
    }

    @Specialization
    protected double doDouble(double value) {
      return value;
    }

    @Specialization
    protected long doBoolean(boolean value) {
      return value ? 1 : 0;
    }

    @Specialization
    protected long doBigInt(BigIntObject value) {
      return getBigIntValue(value.get());
    }

    @Specialization
    protected Object doString(TruffleString string) {
      try {
        return string.parseLongUncached();
      } catch (TruffleString.NumberFormatException e) {
        try {
          return string.parseDoubleUncached();
        } catch (TruffleString.NumberFormatException ex) {
          return 0;
        }
      }
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected long getBigIntValue(BigInteger bigInteger) {
      return bigInteger.intValue();
    }

    protected Object doObject(BladeObject object, @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      return methodOverride(this, "@number", object, interopLibrary, 0);
    }

    @Fallback
    protected long doOthers(Object object) {
      return 0;
    }
  }
}
