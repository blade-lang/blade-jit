package org.zuri.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.zuri.language.nodes.common.NGlobalScopeObjectNode;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.NStmtNode;
import org.zuri.language.runtime.ZuriRuntimeError;

@NodeChild(value = "globalScopeNode", type = NGlobalScopeObjectNode.class)
@NodeChild(value = "value", type = NNode.class)
@NodeField(name = "name", type = String.class)
@NodeField(name = "isConst", type = Boolean.class)
public abstract class NGlobalDeclNode extends NStmtNode {
  @CompilerDirectives.CompilationFinal
  private boolean exists = true;

  protected abstract String getName();

  protected abstract boolean getIsConst();

  @Specialization(limit = "3")
  protected Object create(DynamicObject globalScope, Object value,
                          @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    String name = getName();

    if (exists) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      exists = false;

      if (objectLibrary.containsKey(globalScope, name)) {
        throw ZuriRuntimeError.error(this, "'", name, "' already declared in this scope");
      }
    }

    objectLibrary.putWithFlags(globalScope, name, value, getIsConst() ? 1 : 0);
    return value;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    // Global variables representing class declarations don't provide a SourceSection,
    // since we don't want the debugger to stop on them.
    // For that reason, make sure to return the standard Statement tag only if we have a SourceSection
    return this.getSourceSection() != null && super.hasTag(tag);
  }
}
