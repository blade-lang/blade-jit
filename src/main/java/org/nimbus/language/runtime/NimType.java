package org.nimbus.language.runtime;

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
import org.nimbus.language.NimbusLanguage;

@ExportLibrary(InteropLibrary.class)
public final class NimType implements TruffleObject {
  public static final NimType NUMBER = new NimType("Number", (l, v) -> l.fitsInLong(v) || v instanceof Double);
  public static final NimType NIL = new NimType("Nil", InteropLibrary::isNull);
  public static final NimType STRING = new NimType("String", InteropLibrary::isString);
  public static final NimType LIST = new NimType("List", (l, v) -> v instanceof NListObject);
  public static final NimType BOOLEAN = new NimType("Boolean", InteropLibrary::isBoolean);
  public static final NimType CLASS = new NimType("Class", (l, v) -> v instanceof NimClass);
  public static final NimType OBJECT = new NimType("Object", InteropLibrary::hasMembers);
  public static final NimType FUNCTION = new NimType("Function", InteropLibrary::isExecutable);

  @CompilerDirectives.CompilationFinal(dimensions = 1)
  public static final NimType[] PRECEDENCE = new NimType[]{NIL, BOOLEAN, NUMBER, STRING, LIST, FUNCTION, CLASS, OBJECT};

  @CompilerDirectives.CompilationFinal
  private final String name;

  @CompilerDirectives.CompilationFinal
  private final TypeCheck isInstance;

  private NimType(String name, TypeCheck isInstance) {
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
    return NimbusLanguage.class;
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
    static boolean doCached(@SuppressWarnings("unused") NimType type, Object value,
                            @Cached("type") NimType cachedType,
                            @CachedLibrary("value") InteropLibrary valueLib) {
      return cachedType.isInstance.check(valueLib, value);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(replaces = "doCached")
    static boolean doGeneric(NimType type, Object value) {
      return type.isInstance.check(InteropLibrary.getFactory().getUncached(), value);
    }
  }

  @FunctionalInterface
  interface TypeCheck {
    boolean check(InteropLibrary lib, Object value);
  }
}
