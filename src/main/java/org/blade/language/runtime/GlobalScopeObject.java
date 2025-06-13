package org.blade.language.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import org.blade.language.BladeLanguage;

@ExportLibrary(InteropLibrary.class)
public class GlobalScopeObject extends DynamicObject {

  public GlobalScopeObject(Shape shape) {
    super(shape);
  }

  @ExportMessage
  boolean isMemberReadable(String member,
                           @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return objectLibrary.containsKey(this, member);
  }

  @ExportMessage
  Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return new MemberNamesObject(objectLibrary.getKeyArray(this));
  }

  @ExportMessage
  Object readMember(String member,
                    @CachedLibrary("this") DynamicObjectLibrary objectLibrary) throws UnknownIdentifierException {
    Object value = objectLibrary.getOrDefault(this, member, null);
    if (null == value) {
      throw UnknownIdentifierException.create(member);
    }
    return value;
  }

  @ExportMessage
  boolean isMemberModifiable(String member,
                             @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return objectLibrary.containsKey(this, member);
  }

  @ExportMessage
  boolean isMemberInsertable(String member,
                             @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return !objectLibrary.containsKey(this, member);
  }

  @ExportMessage
  void writeMember(String member, Object value,
                   @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    objectLibrary.put(this, member, value);
  }

  @ExportMessage
  boolean isScope() {
    return true;
  }

  @ExportMessage
  Object getMetaObject() {
    return BladeType.OBJECT;
  }

  @ExportMessage
  boolean hasLanguage() {
    return true;
  }

  @ExportMessage
  boolean hasMembers() {
    return true;
  }

  @ExportMessage
  boolean hasMetaObject() {
    return true;
  }

  @ExportMessage
  Class<? extends TruffleLanguage<?>> getLanguage() {
    return BladeLanguage.class;
  }

  @ExportMessage
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return toString();
  }

  @Override
  public String toString() {
    return "global";
  }

  @ExplodeLoop
  public GlobalScopeObject duplicate(DynamicObjectLibrary objectLibrary) {
    GlobalScopeObject globalScope = new GlobalScopeObject(this.getShape());
    Object[] keys = objectLibrary.getKeyArray(this);

    for (Object key : keys) {
      objectLibrary.put(globalScope, key, objectLibrary.getOrDefault(this, key, null));
    }

    return globalScope;
  }
}
