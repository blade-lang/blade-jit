package org.blade.language.nodes;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.string.NReadStringPropertyNode;
import org.blade.language.runtime.*;

@SuppressWarnings({"truffle-inlining", "truffle-unused"})
@ImportStatic(BladeContext.class)
public abstract class NSharedPropertyReaderNode extends Node {
  public abstract Object executeRead(Object object, Object property);

  @Specialization
  protected Object doString(TruffleString string, Object property,
                            @Cached NReadStringPropertyNode stringPropertyReader) {
    return stringPropertyReader.executeProperty(string, property);
  }

  @Specialization(guards = "interopLibrary.isNull(target)", limit = "3")
  protected Object doNil(
    Object target, Object property,
    @CachedLibrary("target") InteropLibrary interopLibrary
  ) {
    throw BladeRuntimeError.typeError(
      this,
      BString.concatString("Cannot read properties of nil (reading '", property, "')")
    );
  }

  @Specialization
  protected Object doRange(RangeObject target, Object property, @Bind Node node,
                           @Cached("get(node)") BladeContext context,
                           @Cached(value = "context.objectsModel.rangeObject", neverDefault = false) BladeClass rangeObject,
                           @Cached(value = "context.objectsModel.objectObject") BladeClass objectObject,
                           @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") DynamicObjectLibrary objectLibrary
  ) {
    Object value = objectLibrary.getOrDefault(rangeObject, property, null);
    if(value == null) {
      return objectLibrary.getOrDefault(objectObject, property, BladeNil.SINGLETON);
    }
    return value;
  }

  @Specialization
  protected Object doDictionary(DictionaryObject target, Object property, @Bind Node node,
                                @Cached("get(node)") BladeContext context,
                                @Cached(value = "context.objectsModel.dictionaryObject", neverDefault = false) BladeClass dictionaryObject,
                                @Cached(value = "context.objectsModel.objectObject") BladeClass objectObject,
                                @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") DynamicObjectLibrary objectLibrary
  ) {
    Object value = objectLibrary.getOrDefault(dictionaryObject, property, null);
    if(value == null) {
      return objectLibrary.getOrDefault(objectObject, property, BladeNil.SINGLETON);
    }
    return value;
  }

  @Specialization
  protected Object doBigInt(BigIntObject target, Object property, @Bind Node node,
                            @Cached("get(node)") BladeContext context,
                            @Cached(value = "context.objectsModel.bigIntObject", neverDefault = false) BladeClass bigIntObject,
                            @Cached(value = "context.objectsModel.objectObject") BladeClass objectObject,
                            @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") DynamicObjectLibrary objectLibrary
  ) {
    Object value = objectLibrary.getOrDefault(bigIntObject, property, null);
    if(value == null) {
      return objectLibrary.getOrDefault(objectObject, property, BladeNil.SINGLETON);
    }
    return value;
  }

  @Specialization(guards = "interopLibrary.hasMembers(target)", limit = "3")
  protected Object doGeneric(Object target, TruffleString name,
                             @CachedLibrary("target") InteropLibrary interopLibrary) {
    try {
      return interopLibrary.readMember(target, name.toJavaStringUncached());
    } catch (UnknownIdentifierException e) {
      return BladeNil.SINGLETON;
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.error(this, e.getMessage());
    }
  }

  @Fallback
  protected Object doUnknown(Object target, Object property, @Bind Node node,
                             @Cached("get(node)") BladeContext context,
                             @Cached(value = "context.objectsModel.objectObject", neverDefault = false) BObject objectObject,
                             @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") DynamicObjectLibrary objectLibrary
  ) {
    return objectLibrary.getOrDefault(objectObject, property, BladeNil.SINGLETON);
  }
}
