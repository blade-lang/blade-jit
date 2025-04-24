package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.nimbus.language.nodes.NGlobalScopeObjectNode;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;
import org.nimbus.language.runtime.NString;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.runtime.NimRuntimeError;

@NodeChild(value = "globalScopeNode", type = NGlobalScopeObjectNode.class)
@NodeChild(value = "value", type = NNode.class)
@NodeField(name = "name", type = String.class)
@NodeField(name = "isConst", type = Boolean.class)
public abstract class NGlobalDeclNode extends NStmtNode {
  protected abstract String getName();
  protected abstract boolean getIsConst();

  @CompilerDirectives.CompilationFinal
  private boolean exists = true;

  @Specialization(limit = "3")
  protected Object create(DynamicObject globalScope, Object value,
                          @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    String name = getName();

    if (exists) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      exists = false;

      if (objectLibrary.containsKey(globalScope, name)) {
        throw NimRuntimeError.create("'", name, "' already declared in this scope");
      }
    }

    objectLibrary.putWithFlags(globalScope, name, value, getIsConst() ? 1 : 0);
    return value;
  }
}
