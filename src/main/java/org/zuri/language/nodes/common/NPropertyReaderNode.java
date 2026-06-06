package org.zuri.language.nodes.common;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.nodes.NBaseNode;
import org.zuri.language.nodes.string.NStringPropertyReaderNode;
import org.zuri.language.runtime.BObject;
import org.zuri.language.runtime.BString;
import org.zuri.language.runtime.ZuriNil;
import org.zuri.language.runtime.ZuriRuntimeError;

@SuppressWarnings({"truffle-inlining", "truffle-unused"})
public abstract class NPropertyReaderNode extends NBaseNode {
  public abstract Object executeRead(Object object, Object property);

  @Specialization
  protected static Object doString(TruffleString string, Object property,
                            @Cached(neverDefault = true) NStringPropertyReaderNode stringPropertyReader) {
    return stringPropertyReader.executeProperty(string, property);
  }

  @Specialization(guards = "interopLibrary.hasMembers(target)", limit = "3")
  protected static Object doGeneric(Object target, String name, @Bind Node node,
                             @CachedLibrary("target") InteropLibrary interopLibrary) {
    try {
      return interopLibrary.readMember(target, name);
    } catch (UnknownIdentifierException e) {
      return ZuriNil.SINGLETON;
    } catch (UnsupportedMessageException e) {
      throw ZuriRuntimeError.error(node, e.getMessage());
    }
  }

  @Specialization(guards = "interopLibrary.isNull(target)", limit = "3")
  protected static Object doNil(
    Object target, Object property, @Bind Node node,
    @CachedLibrary("target") InteropLibrary interopLibrary
  ) {
    throw ZuriRuntimeError.typeError(
      node,
      BString.concatString("Cannot read properties of nil (reading '", property, "')")
    );
  }

  @Fallback
  protected static Object doUnknown(
    @SuppressWarnings("unused") Object target,
    @SuppressWarnings("unused") Object property,
    @Cached(value = "languageContext().objectsModel.objectObject", neverDefault = false) BObject objectObject,
    @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary
  ) {
    return objectLibrary.getOrDefault(objectObject, BString.toString(property), ZuriNil.SINGLETON);
  }
}
