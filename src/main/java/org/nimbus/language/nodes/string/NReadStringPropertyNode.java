package org.nimbus.language.nodes.string;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.nodes.NBaseNode;
import org.nimbus.language.runtime.*;

@ImportStatic(NString.class)
@SuppressWarnings("truffle-inlining")
public abstract class NReadStringPropertyNode extends NBaseNode {
  public static final String LENGTH_PROP = "length";
  public abstract Object executeProperty(TruffleString self, Object property);

  @Specialization
  protected Object readStringIndex(
    TruffleString string, long index,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
    @Cached(value = "length(string, lengthNode)", neverDefault = false) long stringLength,
    @Cached TruffleString.SubstringNode substringNode
  ) {
    if(index < 0) index = index + stringLength;

    return index < 0 || index >= stringLength
      ? NString.EMPTY
      : NString.substring(string, (int)index, 1, substringNode);
  }

  @Specialization(guards = "LENGTH_PROP.equals(name)")
  protected long readLengthProperty(
    TruffleString string, String name,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
    @Cached(value = "length(string, lengthNode)", neverDefault = false) long stringLength
  ) {
    return stringLength;
  }

  @Fallback
  protected Object readOthers(
    TruffleString string, Object property,
    @Cached(value = "languageContext().objectsModel.stringObject", neverDefault = false) NimClass stringClass,
    @CachedLibrary(limit = "3") InteropLibrary interopLibrary
  ) {
    try {
      return interopLibrary.readMember(stringClass, NString.tryToString(property));
    } catch (UnsupportedMessageException e) {
      throw NimRuntimeError.create(e.getMessage());
    } catch (UnknownIdentifierException e) {
      return NimNil.SINGLETON;
    }
  }
}
