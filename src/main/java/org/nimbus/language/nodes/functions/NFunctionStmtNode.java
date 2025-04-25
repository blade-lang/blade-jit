package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.SourceSection;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NFunctionRootNode;
import org.nimbus.language.nodes.NNode;
import org.nimbus.language.nodes.NStmtNode;
import org.nimbus.language.runtime.NFunctionObject;
import org.nimbus.language.runtime.NimNil;
import org.nimbus.language.shared.NBuiltinClassesModel;

@NodeChild(value = "containerNode", type = NNode.class)
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
  public Object declare(DynamicObject container,
                        @CachedLibrary("container") DynamicObjectLibrary objectLibrary) {
    if (cachedFunction == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();

      NFunctionRootNode function = new NFunctionRootNode(NimbusLanguage.get(this), getFrameDescriptor(), getBody(), getName());
      NBuiltinClassesModel classesModel = languageContext().objectsModel;
      cachedFunction = new NFunctionObject(classesModel.rootShape, classesModel.functionObject, getName(), function.getCallTarget(), getArgumentCount());
    }

    objectLibrary.putConstant(container, getName(), cachedFunction, 0);
    return NimNil.SINGLETON;
  }
}
