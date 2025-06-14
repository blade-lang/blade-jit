package org.blade.language.nodes.string;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NBaseNode;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BladeClass;
import org.blade.language.runtime.BladeNil;
import org.blade.language.runtime.BladeRuntimeError;

@ImportStatic(BString.class)
@SuppressWarnings("truffle-inlining")
public abstract class NReadStringPropertyNode extends NBaseNode {
  public static final String LENGTH_PROP = "length";

  public abstract Object executeProperty(TruffleString self, Object property);

  @Specialization
  protected Object readStringIndex(
    TruffleString string, long index,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
    @Cached TruffleString.SubstringNode substringNode
  ) {
    long stringLength = BString.length(string, lengthNode);
    if (index < 0) index = index + stringLength;

    return index < 0 || index >= stringLength
      ? BString.EMPTY
      : BString.substring(string, (int) index, 1, substringNode);
  }

  @Specialization(guards = "LENGTH_PROP.equals(name)")
  protected long readLengthProperty(
    TruffleString string, String name,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode
  ) {
    return BString.length(string, lengthNode);
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
      throw BladeRuntimeError.error(this, e.getMessage());
    } catch (UnknownIdentifierException e) {
      return BladeNil.SINGLETON;
    }
  }
}
