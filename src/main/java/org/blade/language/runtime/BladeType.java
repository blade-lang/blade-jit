package org.blade.language.runtime;

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
import org.blade.language.BladeLanguage;

@ExportLibrary(InteropLibrary.class)
public final class BladeType implements TruffleObject {
  public static final BladeType NUMBER = new BladeType("Number", (l, v) -> l.fitsInLong(v) || v instanceof Double);
  public static final BladeType NIL = new BladeType("Nil", InteropLibrary::isNull);
  public static final BladeType STRING = new BladeType("String", InteropLibrary::isString);
  public static final BladeType LIST = new BladeType("List", (l, v) -> v instanceof ListObject);
  public static final BladeType BOOLEAN = new BladeType("Boolean", InteropLibrary::isBoolean);
  public static final BladeType CLASS = new BladeType("Class", (l, v) -> v instanceof BladeClass);
  public static final BladeType OBJECT = new BladeType("Object", InteropLibrary::hasMembers);
  public static final BladeType FUNCTION = new BladeType("Function", InteropLibrary::isExecutable);

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  public static final BladeType[] PRECEDENCE = new BladeType[]{NIL, BOOLEAN, NUMBER, STRING, LIST, FUNCTION, CLASS, OBJECT};

  @CompilerDirectives.CompilationFinal
  private final String name;

  @CompilerDirectives.CompilationFinal
  private final TypeCheck isInstance;

  private BladeType(String name, TypeCheck isInstance) {
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
    return BladeLanguage.class;
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

  @ExportMessage
  static class IsMetaInstance {

    @Specialization(guards = "type == cachedType", limit = "3")
    static boolean doCached(@SuppressWarnings("unused") BladeType type, Object value,
                            @Cached("type") BladeType cachedType,
                            @CachedLibrary("value") InteropLibrary valueLib) {
      return cachedType.isInstance.check(valueLib, value);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(replaces = "doCached")
    static boolean doGeneric(BladeType type, Object value) {
      return type.isInstance.check(InteropLibrary.getFactory().getUncached(), value);
    }
  }

  @FunctionalInterface
  interface TypeCheck {
    boolean check(InteropLibrary lib, Object value);
  }
}
