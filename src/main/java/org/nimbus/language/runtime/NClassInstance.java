package org.nimbus.language.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.NimbusLanguage;

@ExportLibrary(InteropLibrary.class)
public class NClassInstance extends DynamicObject {
  final NClassObject classObject;

  public NClassInstance(Shape shape, NClassObject classObject) {
    super(shape);
    this.classObject = classObject;
  }

  @Override
  public String toString() {
    return String.format("<class %s instance at 0x%x>", classObject.name, classObject.hashCode());
  }

  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return toString();
  }

  @ExportMessage
  boolean hasMembers() {
    return true;
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
  boolean isMemberReadable(String member,
                           @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                           @CachedLibrary("this.classObject") DynamicObjectLibrary classObjectLibrary) {
    return instanceObjectLibrary.containsKey(this, member) ||
      classObjectLibrary.containsKey(classObject, member);
  }

  @ExportMessage
  boolean isMemberModifiable(String member,
                             @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                             @CachedLibrary("this.classObject") DynamicObjectLibrary classObjectLibrary) {
    return isMemberReadable(member, instanceObjectLibrary, classObjectLibrary);
  }

  @ExportMessage
  boolean isMemberInsertable(String member,
                             @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                             @CachedLibrary("this.classObject") DynamicObjectLibrary classObjectLibrary) {
    return !isMemberModifiable(member, instanceObjectLibrary, classObjectLibrary);
  }

  @ExportMessage
  Object readMember(String member,
                    @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                    @CachedLibrary("this.classObject") DynamicObjectLibrary classObjectLibrary)
    throws UnknownIdentifierException {
    Object value = instanceObjectLibrary.getOrDefault(this, member, null);
    if (value == null) {
      value = classObjectLibrary.getOrDefault(classObject, member, null);
    }
    if (value == null) {
      throw UnknownIdentifierException.create(member);
    }
    return value;
  }

  @ExportMessage
  Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return new NMemberNamesObject(objectLibrary.getKeyArray(this));
  }

  @ExportMessage
  void writeMember(String member, Object value,
                   @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    objectLibrary.put(this, member, value);
  }

  public String getClassName() {
    return classObject.name;
  }
}
