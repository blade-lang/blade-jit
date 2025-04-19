package org.nimbus.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.string.NReadStringPropertyNode;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

@SuppressWarnings("truffle-inlining")
public abstract class NSharedPropertyReaderNode extends Node {
  public abstract Object executeRead(Object object, Object property);

  @Specialization
  protected Object doString(TruffleString string, Object property,
                            @Cached NReadStringPropertyNode stringPropertyReader) {
    return  stringPropertyReader.executeProperty(string, property);
  }

  @Specialization(guards = "interopLibrary.hasMembers(target)", limit = "3")
  protected Object doGeneric(Object target, String name,
                             @CachedLibrary("target") InteropLibrary interopLibrary) {
    try {
      return interopLibrary.readMember(target, name);
    } catch (UnknownIdentifierException e) {
      return NimNil.SINGLETON;
    } catch (UnsupportedMessageException e) {
      throw new NimRuntimeError(e.getMessage());
    }
  }

  @Specialization(guards = "interopLibrary.isNull(target)", limit = "3")
  protected Object doNil(Object target, Object property,
                         @CachedLibrary("target") InteropLibrary interopLibrary) {
    throw new NimRuntimeError("Cannot read properties of nil (reading '" + property + "')");
  }

  @Fallback
  protected Object doUnknown(Object target, Object property) {
    throw new NimRuntimeError("Object of type does not carry properties.");
  }
}
