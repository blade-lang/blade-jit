package org.blade.language.nodes.string;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NBaseNode;
import org.blade.language.runtime.*;

@ImportStatic(BString.class)
@SuppressWarnings("truffle-inlining")
public abstract class NReadStringPropertyNode extends NBaseNode {
  public static final String LENGTH_PROP = "length";
  public abstract Object executeProperty(TruffleString self, Object property);

  @Specialization
  protected Object readStringIndex(
    TruffleString string, int index,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
    @Cached(value = "length(string, lengthNode)", neverDefault = false) int stringLength,
    @Cached TruffleString.SubstringNode substringNode
  ) {
    if(index < 0) index = index + stringLength;

    return index < 0 || index >= stringLength
      ? BString.EMPTY
      : BString.substring(string, (int)index, 1, substringNode);
  }

  @Specialization(guards = "LENGTH_PROP.equals(name)")
  protected int readLengthProperty(
    TruffleString string, String name,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
    @Cached(value = "length(string, lengthNode)", neverDefault = false) int stringLength
  ) {
    return stringLength;
  }

  @Fallback
  protected Object readOthers(
    TruffleString string, Object property,
    @Cached(value = "languageContext().objectsModel.stringObject", neverDefault = false) BladeClass stringClass,
    @CachedLibrary(limit = "3") InteropLibrary interopLibrary
  ) {
    try {
      return interopLibrary.readMember(stringClass, BString.tryToString(property));
    } catch (UnsupportedMessageException e) {
      throw BladeRuntimeError.create(e.getMessage());
    } catch (UnknownIdentifierException e) {
      return BladeNil.SINGLETON;
    }
  }
}
