package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.runtime.BladeRuntimeError;

@GenerateInline
@GenerateUncached
public abstract class NGetGlobalNode extends Node {

  public static NGetGlobalNode getUncached() {
    return NGetGlobalNodeGen.getUncached();
  }

  @NeverDefault
  public static NGetGlobalNode create() {
    return NGetGlobalNodeGen.create();
  }

  public abstract Object executeGet(Node node, Object globals, TruffleString name);

  @Specialization(limit = "3")
  protected Object read(Node node, DynamicObject globalScope, TruffleString name,
                        @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    Object value = objectLibrary.getOrDefault(globalScope, name, null);
    if (value == null) {
      throw BladeRuntimeError.error(this, "'", name, "' is not defined in this scope");
    }
    return value;
  }
}
