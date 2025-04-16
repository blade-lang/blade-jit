package org.nimbus.language.nodes.statements;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NFunctionRootNode;
import org.nimbus.language.nodes.NGlobalScopeObjectNode;
import org.nimbus.language.nodes.NStmtNode;
import org.nimbus.language.runtime.NFunctionObject;
import org.nimbus.language.runtime.NimNil;

@NodeChild(value = "globalScopeNode", type = NGlobalScopeObjectNode.class)
@NodeField(name = "name", type = String.class)
@NodeField(name = "frameDescriptor", type = FrameDescriptor.class)
@NodeField(name = "body", type = NFunctionBodyNode.class)
@NodeField(name = "argumentCount", type = int.class)
public abstract class NFunctionStmtNode extends NStmtNode {
  protected abstract String getName();
  protected abstract FrameDescriptor getFrameDescriptor();
  protected abstract NFunctionBodyNode getBody();
  protected abstract int getArgumentCount();

  @CompilerDirectives.CompilationFinal
  private NFunctionObject cachedFunction = null;

  @Specialization(limit = "3")
  public Object declare(DynamicObject globalScope,
                        @CachedLibrary("globalScope") DynamicObjectLibrary objectLibrary) {
    if (cachedFunction == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();

      NFunctionRootNode function = new NFunctionRootNode(NimbusLanguage.get(this), getFrameDescriptor(), getBody());
      cachedFunction = new NFunctionObject(getName(), function.getCallTarget(), getArgumentCount());
    }

    objectLibrary.putConstant(globalScope, getName(), cachedFunction, 0);
    return NimNil.SINGLETON;
  }
}
