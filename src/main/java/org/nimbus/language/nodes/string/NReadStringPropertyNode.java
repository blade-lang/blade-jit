package org.nimbus.language.nodes.string;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.runtime.*;

@ImportStatic(NString.class)
@SuppressWarnings("truffle-inlining")
public abstract class NReadStringPropertyNode extends Node {
  protected static final String LENGTH_PROP = "length";
  protected static final String UPPER_PROP = "upper";
  protected static final String INDEX_OF_PROP = "index_of";
  public abstract Object executeProperty(TruffleString self, Object property);

  @Specialization
  protected Object readStringIndex(
    TruffleString string, long index,
    @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
    @Cached(value = "length(string, lengthNode)") long stringLength,
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

  @Specialization(guards = {
    "INDEX_OF_PROP.equals(name)",
    "same(indexOfMethod.methodTarget, string)"
  }, limit = "3")
  protected NFunctionObject cachedReadIndexOfProperty(
    TruffleString string, String name,
    @Cached("createStringIndexOfMethod(string)") NFunctionObject indexOfMethod
  ) {
    return indexOfMethod;
  }

  @Specialization(guards = "INDEX_OF_PROP.equals(name)", replaces = "cachedReadIndexOfProperty")
  protected NFunctionObject readIndexOfProperty(TruffleString string, String name) {
    return createStringIndexOfMethod(string);
  }

  @Specialization(guards = {
    "UPPER_PROP.equals(name)",
    "same(indexOfMethod.methodTarget, string)"
  }, limit = "3")
  protected NFunctionObject cachedReadUpperProperty(
    TruffleString string, String name,
    @Cached("createStringUpperMethod(string)") NFunctionObject indexOfMethod
  ) {
    return indexOfMethod;
  }

  @Specialization(guards = "UPPER_PROP.equals(name)", replaces = "cachedReadUpperProperty")
  protected NFunctionObject readUpperProperty(TruffleString string, String name) {
    return createStringUpperMethod(string);
  }

  protected NFunctionObject createStringIndexOfMethod(TruffleString string) {
    return new NFunctionObject(INDEX_OF_PROP, NimContext.get(this).stringPrototype.indexOfMethod, 3, string);
  }

  protected NFunctionObject createStringUpperMethod(TruffleString string) {
    return new NFunctionObject(UPPER_PROP, NimContext.get(this).stringPrototype.upperMethod, 1, string);
  }

  @Fallback
  protected Object readUnknownProperty(TruffleString string, Object property) {
    throw new NimRuntimeError("class String has no named property '" + property + "'");
  }
}
