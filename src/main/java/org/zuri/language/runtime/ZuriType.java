package org.zuri.language.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.zuri.language.ZuriLanguage;

@ExportLibrary(InteropLibrary.class)
public final class ZuriType implements TruffleObject {
  public static final ZuriType NUMBER = new ZuriType(
    "Number",
    (l, v) -> l.fitsInLong(v) || v instanceof Double || v instanceof BigIntObject
  );
  public static final ZuriType BIGINT = new ZuriType("BigInt", (l, v) -> v instanceof BigIntObject);
  public static final ZuriType NIL = new ZuriType("Nil", InteropLibrary::isNull);
  public static final ZuriType STRING = new ZuriType("String", InteropLibrary::isString);
  public static final ZuriType LIST = new ZuriType("List", (l, v) -> v instanceof ListObject);
  public static final ZuriType DICTIONARY = new ZuriType("Dictionary", (l, v) -> v instanceof DictionaryObject);
  public static final ZuriType BOOLEAN = new ZuriType("Bool", InteropLibrary::isBoolean);
  public static final ZuriType CLASS = new ZuriType("Class", (l, v) -> v instanceof ZuriClass);
  public static final ZuriType OBJECT = new ZuriType("Object", InteropLibrary::hasMembers);
  public static final ZuriType FUNCTION = new ZuriType("Function", InteropLibrary::isExecutable);

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  public static final ZuriType[] PRECEDENCE = new ZuriType[]{NIL, BOOLEAN, NUMBER, BIGINT, STRING, LIST, DICTIONARY, FUNCTION, CLASS, OBJECT};

  @CompilerDirectives.CompilationFinal
  private final String name;

  @CompilerDirectives.CompilationFinal
  private final TypeCheck isInstance;

  private ZuriType(String name, TypeCheck isInstance) {
    this.name = name;
    this.isInstance = isInstance;
  }

  public boolean isInstance(Object value, InteropLibrary interop) {
    CompilerAsserts.partialEvaluationConstant(this);
    return isInstance.check(interop, value);
  }

  @ExportMessage
  boolean hasLanguage() {
    return true;
  }

  @ExportMessage
  Class<? extends TruffleLanguage<?>> getLanguage() {
    return ZuriLanguage.class;
  }

  @ExportMessage
  boolean isMetaObject() {
    return true;
  }

  @ExportMessage(name = "getMetaQualifiedName")
  @ExportMessage(name = "getMetaSimpleName")
  public Object getName() {
    return name;
  }

  @ExportMessage(name = "toDisplayString")
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return name;
  }

  @CompilerDirectives.TruffleBoundary
  @Override
  public String toString() {
    return "RemType[" + name + "]";
  }

  @FunctionalInterface
  interface TypeCheck {
    boolean check(InteropLibrary lib, Object value);
  }

  @ExportMessage
  static class IsMetaInstance {

    @Specialization(guards = "type == cachedType", limit = "3")
    static boolean doCached(@SuppressWarnings("unused") ZuriType type, Object value,
                            @Cached("type") ZuriType cachedType,
                            @CachedLibrary("value") InteropLibrary valueLib) {
      return cachedType.isInstance.check(valueLib, value);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(replaces = "doCached")
    static boolean doGeneric(ZuriType type, Object value) {
      return type.isInstance.check(InteropLibrary.getFactory().getUncached(), value);
    }
  }
}
