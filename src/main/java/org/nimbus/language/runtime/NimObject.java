package org.nimbus.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.NimbusLanguage;

@ExportLibrary(InteropLibrary.class)
public class NimObject extends DynamicObject {
  public final DynamicObject classObject;

  public NimObject(Shape shape, DynamicObject classObject) {
    super(shape);
    this.classObject = classObject;
  }

  @CompilerDirectives.TruffleBoundary
  @Override
  public String toString() {
    return NString.format("<class %s instance at 0x%x>", ((NimClass) classObject).name, hash());
  }

  @CompilerDirectives.TruffleBoundary
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
  boolean hasMetaObject() {
    return true;
  }

  @ExportMessage
  Object getMetaObject() throws UnsupportedMessageException {
    return NimType.OBJECT;
  }

  @ExportMessage
  boolean isMemberReadable(String member,
                           @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                           @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary) {
    return instanceObjectLibrary.containsKey(this, member) ||
      classInteropLibrary.isMemberReadable(classObject, member);
  }

  @ExportMessage
  boolean isMemberModifiable(String member,
                             @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                             @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary) {
    return isMemberReadable(member, instanceObjectLibrary, classInteropLibrary);
  }

  @ExportMessage
  boolean isMemberInsertable(String member,
                             @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                             @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary) {
    return !isMemberModifiable(member, instanceObjectLibrary, classInteropLibrary);
  }

  @ExportMessage
  Object readMember(
    String member,
    @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
    @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary
  ) throws UnknownIdentifierException, UnsupportedMessageException {
    Object value = instanceObjectLibrary.getOrDefault(this, member, null);
    if (value == null) {
      value = classInteropLibrary.readMember(classObject, member);
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

  @CompilerDirectives.TruffleBoundary
  public String getClassName() {
    return ((NimClass) classObject).name;
  }

  @CompilerDirectives.TruffleBoundary
  public int hash() {
    return hashCode();
  }
}
