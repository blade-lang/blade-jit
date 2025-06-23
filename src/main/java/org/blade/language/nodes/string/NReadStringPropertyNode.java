package org.blade.language.nodes.string;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NBaseNode;
import org.blade.language.nodes.list.NReadListIndexNode;
import org.blade.language.nodes.list.NReadListIndexNodeGen;
import org.blade.language.runtime.*;

@ImportStatic({BString.class, BladeContext.class})
@SuppressWarnings("truffle-inlining")
public abstract class NReadStringPropertyNode extends Node {

  public static NReadStringPropertyNode getUncached() {
    return NReadStringPropertyNodeGen.getUncached();
  }

  @NeverDefault
  public static NReadStringPropertyNode create() {
    return NReadStringPropertyNodeGen.create();
  }

  public static final TruffleString LENGTH_PROP = BString.fromJavaString("length");

  public abstract Object executeProperty(Object self, Object property);

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

  @Specialization(guards = "equals(LENGTH_PROP, name, equalNode)", limit = "1")
  protected long readLengthProperty(
    TruffleString string, TruffleString name,
    @Cached TruffleString.EqualNode equalNode,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode
  ) {
    return BString.length(string, lengthNode);
  }

  @Fallback
  protected Object readOthers(
    Object object, Object property, @Bind Node node,
    @Cached("get(node)") BladeContext context,
    @Cached(value = "context.objectsModel.stringObject", neverDefault = false) BladeClass stringClass,
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
