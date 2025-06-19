package org.blade.language.nodes.statements;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;

@GenerateInline
@GenerateUncached
public abstract class NGlobalDeclNode extends Node {

  public static NGlobalDeclNode getUncached() {
    return NGlobalDeclNodeGen.getUncached();
  }

  @NeverDefault
  public static NGlobalDeclNode create() {
    return NGlobalDeclNodeGen.create();
  }

  public abstract Object executeGlobal(Node node, Object globals, TruffleString name, Object value, boolean isConstant);

  @Specialization(limit = "1", guards = {"cachedGlobalScope == globalScope", "isConstant"})
  public static Object createCachedConstant(Node node, DynamicObject globalScope, TruffleString name, Object value, boolean isConstant,
                          @Cached("globalScope") DynamicObject cachedGlobalScope,
                          @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    objectLibrary.putWithFlags(globalScope, name, value, 1);
    return value;
  }

  @Specialization(limit = "1", guards = {"cachedGlobalScope == globalScope", "!isConstant"})
  public static Object createCachedVariable(Node node, DynamicObject globalScope, TruffleString name, Object value, boolean isConstant,
                          @Cached("globalScope") DynamicObject cachedGlobalScope,
                          @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    objectLibrary.putWithFlags(globalScope, name, value, 0);
    return value;
  }

  @Specialization(limit = "1", replaces = {"createCachedConstant", "createCachedVariable"})
  public static Object createUncachedConstant(Node node, DynamicObject globalScope, TruffleString name, Object value, boolean isConstant,
                          @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    objectLibrary.putWithFlags(globalScope, name, value, isConstant ? 1 : 0);
    return value;
  }
}
