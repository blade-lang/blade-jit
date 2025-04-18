package org.nimbus.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

@ExportLibrary(InteropLibrary.class)
public class NClassInstance implements TruffleObject {
  final NClassObject classObject;

  public NClassInstance(NClassObject classObject) {
    this.classObject = classObject;
  }

  @Override
  public String toString() {
    return String.format("<class(%s) instance>", classObject.name);
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
  boolean isMemberReadable(String member, @CachedLibrary("this.classObject") DynamicObjectLibrary objectLibrary) {
    return objectLibrary.containsKey(classObject, member);
  }

  @ExportMessage
  Object readMember(String member,
                    @CachedLibrary("this.classObject") DynamicObjectLibrary objectLibrary
  ) throws UnknownIdentifierException {
    Object value = objectLibrary.getOrDefault(classObject, member, null);
    if (value == null) {
      throw UnknownIdentifierException.create(member);
    }
    return value;
  }

  @ExportMessage
  Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @CachedLibrary("this.classObject") DynamicObjectLibrary objectLibrary
  ) {
    return new NMemberNamesObject(objectLibrary.getKeyArray(classObject));
  }

  public String getClassName() {
    return classObject.name;
  }
}
