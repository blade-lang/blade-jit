package org.nimbus.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.string.NReadStringPropertyNode;
import org.nimbus.language.runtime.*;
import org.nimbus.language.shared.NBuiltinClassesModel;

@SuppressWarnings({"truffle-inlining", "truffle-unused"})
public abstract class NSharedPropertyReaderNode extends NBaseNode {
  public abstract Object executeRead(Object object, Object property);

  @Specialization
  protected Object doString(TruffleString string, Object property,
                            @Cached NReadStringPropertyNode stringPropertyReader) {
    return stringPropertyReader.executeProperty(string, property);
  }

  @Specialization(guards = "interopLibrary.hasMembers(target)", limit = "3")
  protected Object doGeneric(Object target, String name,
                             @CachedLibrary("target") InteropLibrary interopLibrary) {
    try {
      return interopLibrary.readMember(target, name);
    } catch (UnknownIdentifierException e) {
      return NimNil.SINGLETON;
    } catch (UnsupportedMessageException e) {
      throw NimRuntimeError.create(e.getMessage());
    }
  }

  @Specialization(guards = "interopLibrary.isNull(target)", limit = "3")
  protected Object doNil(
    Object target, Object property,
    @CachedLibrary("target") InteropLibrary interopLibrary,
    @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") DynamicObjectLibrary objectLibrary,
    @Cached(value = "languageContext().objectsModel", neverDefault = true) NBuiltinClassesModel classesModel
  ) {
    throw NimRuntimeError.typeError(this, NString.concatString("Cannot read properties of nil (reading '", property, "')"));
  }

  @Fallback
  protected Object doUnknown(
    @SuppressWarnings("unused") Object target,
    @SuppressWarnings("unused") Object property,
    @Cached(value = "languageContext().objectsModel.objectObject", neverDefault = true) NObject objectObject,
    @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") DynamicObjectLibrary dynamicObjectLibrary
  ) {
    return dynamicObjectLibrary.getOrDefault(objectObject, NString.toString(property), NimNil.SINGLETON);
  }
}
