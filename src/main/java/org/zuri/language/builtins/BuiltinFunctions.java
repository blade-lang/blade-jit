package org.zuri.language.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.BaseBuiltinDeclaration;
import org.zuri.language.ZuriLanguage;
import org.zuri.language.nodes.common.NToStringNode;
import org.zuri.language.nodes.functions.NBuiltinFunctionNode;
import org.zuri.language.runtime.*;
import org.zuri.language.shared.BuiltinClassesModel;
import org.zuri.utility.RegulatedMap;

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
      add("is_iterable", false, BuiltinFunctionsFactory.IsIterableNodeFactory.getInstance());
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
                         @Cached(value = "languageContext()", neverDefault = false) ZuriContext context) {
      print(context, interopLibrary, object.getItems());
      return ZuriNil.SINGLETON;
    }

    @Fallback
    protected Object fallback(Object object) {
      ZuriContext.get(this).println(ZString.concatString("Something not working right: ", object));
      return ZuriNil.SINGLETON;
    }

    @ExplodeLoop
    private void print(ZuriContext context, InteropLibrary interopLibrary, Object[] arguments) {
      final int length = arguments.length;

      if (length > 0) {
        for (int i = 0; i < length - 1; i++) {
          if (arguments[i] != ZuriNil.SINGLETON) {
            context.print(ZString.fromObject(interopLibrary, arguments[i]));
            context.print(" ");
          }
        }

        final int lengthMinusOne = length - 1;
        if (arguments[lengthMinusOne] != ZuriNil.SINGLETON) {
          context.print(ZString.fromObject(interopLibrary, arguments[lengthMinusOne]));
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
    protected Object doObject(ZuriObject arg, @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      return methodOverride(this, "@abs", arg, interopLibrary, Double.NaN);
    }

    @Fallback
    protected double notANumber(Object object) {
      return Double.NaN;
    }
  }

  public abstract static class BinFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected static TruffleString doLong(long arg, @Bind Node node, @Cached NToStringNode toStringNode) {
      return toStringNode.execute(node, Long.toBinaryString(arg));
    }

    @Fallback
    protected static double doInvalid(Object object, @Bind Node node) {
      throw ZuriRuntimeError.argumentError(node, "bin", object);
    }
  }

  public abstract static class ChrFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected TruffleString doLong(long arg,
                                   @Cached TruffleString.FromCodePointNode fromCodePointNode,
                                   @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
      if (arg >= 0x110000) {
        throw ZuriRuntimeError.valueError(this, "chr() argument out of maximum UTF-16 character range 0x10FFFE");
      }
      return ZString.fromCodePoint(fromCodePointNode, (int) arg);
    }

    @Fallback
    protected double doInvalid(Object object) {
      throw ZuriRuntimeError.argumentError(this, "chr", object);
    }
  }

  public abstract static class HexFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected static TruffleString doLong(long arg, @Bind Node node, @Cached NToStringNode toStringNode) {
      return toStringNode.execute(node, Long.toHexString(arg));
    }

    @Fallback
    protected static double doInvalid(Object object, @Bind Node node) {
      throw ZuriRuntimeError.argumentError(node, "hex", object);
    }
  }

  public abstract static class IdFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected long doNimObject(ZuriObject arg) {
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
    protected boolean doObject(ZuriObject object, ZuriClass testClass,
                               @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                               @Cached(value = "objectsModel.objectObject", neverDefault = true) ZuriClass objectObject) {
      ZuriObject klassObject = (ZuriObject) object.classObject;
      if (klassObject == testClass) return true;

      while (klassObject != null && klassObject != objectObject) {
        if (klassObject.classObject == testClass) return true;
        klassObject = (ZuriObject) klassObject.classObject;
      }

      return false;
    }

    @Specialization
    protected boolean doString(TruffleString string, ZuriClass testClass,
                               @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                               @Cached(value = "objectsModel.objectObject", neverDefault = true) ZuriClass objectObject) {
      return testClass == objectsModel.stringObject || testClass == objectObject;
    }

    @Specialization
    protected boolean doLong(long value, ZuriClass testClass,
                             @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                             @Cached(value = "objectsModel.objectObject", neverDefault = true) ZuriClass objectObject) {
      return testClass == objectsModel.numberObject || testClass == objectObject;
    }

    @Specialization
    protected boolean doDouble(double value, ZuriClass testClass,
                               @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                               @Cached(value = "objectsModel.objectObject", neverDefault = true) ZuriClass objectObject) {
      return testClass == objectsModel.numberObject || testClass == objectObject;
    }

    @Specialization
    protected boolean doBoolean(boolean value, ZuriClass testClass,
                                @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("objectsModel") BuiltinClassesModel objectsModel,
                                @Cached(value = "objectsModel.objectObject", neverDefault = true) ZuriClass objectObject) {
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
      return compareNode.execute(left, right, ZuriLanguage.ENCODING) > 0 ? left : right;
    }

    @Fallback
    protected double doInvalid(Object left, Object right) {
      throw ZuriRuntimeError.argumentError(this, "max", left, right);
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
      return compareNode.execute(left, right, ZuriLanguage.ENCODING) > 0 ? right : left;
    }

    @Fallback
    protected double doInvalid(Object left, Object right) {
      throw ZuriRuntimeError.argumentError(this, "max", left, right);
    }
  }

  public abstract static class OctFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected static TruffleString doLong(long arg, @Bind Node node, @Cached NToStringNode toStringNode) {
      return toStringNode.execute(node, Long.toString(arg, 8));
    }

    @Fallback
    protected static double doInvalid(Object object, @Bind Node node) {
      throw ZuriRuntimeError.argumentError(node, "oct", object);
    }
  }

  public abstract static class OrdFunctionNode extends NBuiltinFunctionNode {
    @Specialization
    protected long doLong(TruffleString string,
                          @Cached TruffleString.CodePointAtIndexNode codePointNode,
                          @Cached TruffleString.CodePointLengthNode lengthNode) {
      long stringLength = ZString.length(string, lengthNode);
      if (stringLength != 1) {
        throw ZuriRuntimeError.valueError(
          this,
          "ord() expected a character, but string of length ",
          stringLength,
          " given"
        );
      }
      return ZString.toCodePoint(string, codePointNode, 0);
    }

    @Fallback
    protected double doInvalid(Object object) {
      throw ZuriRuntimeError.argumentError(this, "ord", object);
    }
  }

  public abstract static class RandFunctionNode extends NBuiltinFunctionNode {
    private SecureRandom secureRandom = null;

    @CompilerDirectives.TruffleBoundary
    @Specialization
    protected double doNilNilNil(ZuriNil min, ZuriNil max, ZuriNil secure) {
      return ThreadLocalRandom.current().nextDouble();
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "secure == true")
    protected double doTrueNilNil(boolean secure, ZuriNil min, ZuriNil max) {
      return getSecureRandom().nextDouble();
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "secure == false")
    protected double doFalseNilNil(boolean secure, ZuriNil min, ZuriNil max) {
      return ThreadLocalRandom.current().nextDouble();
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization
    protected long doLongNilNil(long max, ZuriNil ignored, ZuriNil secure) {
      return ThreadLocalRandom.current().nextLong(max);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "min < max")
    protected long doLongLongNil(long min, long max, ZuriNil secure) {
      return ThreadLocalRandom.current().nextLong(min, max);
    }

    @Specialization(guards = "max <= min")
    protected Object doLongLongNilInvalid(long min, long max, ZuriNil secure) {
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
    protected double doDoubleNilNil(double max, ZuriNil ignored, ZuriNil secure) {
      return ThreadLocalRandom.current().nextDouble(max);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "min < max")
    protected double doDoubleDoubleNil(double min, double max, ZuriNil secure) {
      return ThreadLocalRandom.current().nextDouble(min, max);
    }

    @Specialization(guards = "max <= min")
    protected Object doDoubleDoubleNilInvalid(double min, double max, ZuriNil secure) {
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
      throw ZuriRuntimeError.valueError(this, "ranged rand() requires that min value < max boundary");
    }

    @Fallback
    protected double invalid(Object min, Object max, Object secure) {
      throw ZuriRuntimeError.argumentError(this, "rand()", min, max, secure);
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

  @ImportStatic(ZString.class)
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

    protected Object doObject(ZuriObject object, @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      return methodOverride(this, "@number", object, interopLibrary, 0);
    }

    @Fallback
    protected Object doOthers(Object object) {
      return Double.NaN;
    }
  }

  public abstract static class IsIterableNode extends NBuiltinFunctionNode {
    @Specialization
    protected boolean doBoolean(boolean value) {
      return false;
    }

    @Specialization
    protected boolean doLong(long value) {
      return false;
    }

    @Specialization
    protected boolean doDouble(double value) {
      return false;
    }

    @Specialization
    protected boolean doBigInt(BigIntObject value) {
      return false;
    }

    @Specialization
    protected boolean doList(ListObject value) {
      return true;
    }

    @Specialization
    protected boolean doDict(DictionaryObject value) {
      return true;
    }

    @Specialization
    protected boolean doRange(RangeObject value) {
      return true;
    }

    @Specialization
    protected boolean doRange(ZuriObject value, @CachedLibrary(limit = "3") InteropLibrary interopLibrary) {
      return interopLibrary.isMemberReadable(value, "@key")
        && interopLibrary.isMemberReadable(value, "@value");
    }

    @Fallback
    protected boolean doOthers(Object value) {
      return false;
    }
  }
}
