package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;

@ExportLibrary(InteropLibrary.class)
public class BladeObject extends DynamicObject {
  public final DynamicObject classObject;

  public BladeObject(Shape shape, DynamicObject classObject) {
    super(shape);
    this.classObject = classObject;
  }

  @CompilerDirectives.TruffleBoundary
  @Override
  public String toString() {
    return BString.format("<class %s instance at 0x%x>", ((BladeClass) classObject).name, hash());
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
    return BladeLanguage.class;
  }

  @ExportMessage
  boolean hasMetaObject() {
    return true;
  }

  @ExportMessage
  Object getMetaObject() throws UnsupportedMessageException {
    return BladeType.OBJECT;
  }

  @ExportMessage
  boolean isMemberReadable(String member,
                           @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode,
                           @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                           @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary) {
    return instanceObjectLibrary.containsKey(this, BString.fromObject(fromJavaStringNode, member)) ||
      classInteropLibrary.isMemberReadable(classObject, member);
  }

  @ExportMessage
  boolean isMemberModifiable(String member,
                             @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode,
                             @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                             @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary) {
    return isMemberReadable(member, fromJavaStringNode, instanceObjectLibrary, classInteropLibrary);
  }

  @ExportMessage
  boolean isMemberInsertable(String member,
                             @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode,
                             @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
                             @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary) {
    return !isMemberModifiable(member, fromJavaStringNode, instanceObjectLibrary, classInteropLibrary);
  }

  @ExportMessage
  Object readMember(
    String member,
    @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode,
    @CachedLibrary("this") DynamicObjectLibrary instanceObjectLibrary,
    @CachedLibrary("this.classObject") InteropLibrary classInteropLibrary
  ) throws UnknownIdentifierException, UnsupportedMessageException {
    Object value = instanceObjectLibrary.getOrDefault(this, BString.fromObject(fromJavaStringNode, member), null);
    if (value == null) {
      value = classInteropLibrary.readMember(classObject, member);
    }
    return value;
  }

  @ExportMessage
  MemberNamesObject getMembers(@SuppressWarnings("unused") boolean includeInternal,
                               @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    return new MemberNamesObject(objectLibrary.getKeyArray(this));
  }

  @ExportMessage
  public void writeMember(String member, Object value,
                          @Cached @Cached.Shared("fromJavaStringNode") TruffleString.FromJavaStringNode fromJavaStringNode,
                          @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
    objectLibrary.put(this, BString.fromObject(fromJavaStringNode, member), value);
  }

  @CompilerDirectives.TruffleBoundary
  public String getClassName() {
    return ((BladeClass) classObject).name.toJavaStringUncached();
  }

  @CompilerDirectives.TruffleBoundary
  public long hash() {
    return hashCode();
  }
}
