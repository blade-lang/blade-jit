package org.blade.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NFunctionRootNode;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NStmtNode;
import org.blade.language.runtime.BladeClass;
import org.blade.language.runtime.BladeNil;
import org.blade.language.runtime.FunctionObject;
import org.blade.language.shared.BuiltinClassesModel;

@NodeChild(value = "containerNode", type = NNode.class)
@NodeField(name = "name", type = String.class)
@NodeField(name = "frameDescriptor", type = FrameDescriptor.class)
@NodeField(name = "body", type = NFunctionBodyNode.class)
@NodeField(name = "argumentCount", type = int.class)
@NodeField(name = "isVariadic", type = int.class)
@ImportStatic(BladeLanguage.class)
public abstract class NFunctionStmtNode extends NStmtNode {
  protected abstract String getName();
  protected abstract FrameDescriptor getFrameDescriptor();
  protected abstract NFunctionBodyNode getBody();
  protected abstract int getArgumentCount();
  protected abstract int getIsVariadic();

  @CompilerDirectives.CompilationFinal
  private FunctionObject cachedFunction = null;

  @Specialization(limit = "3")
  public Object declare(DynamicObject container,
                        @Bind Node node,
                        @Cached("get(node)") BladeLanguage language,
                        @Cached("languageContext().objectsModel") BuiltinClassesModel classesModel,
                        @Cached("classesModel.functionObject") BladeClass functionObject,
                        @Cached("classesModel.rootShape") Shape rootShape,
                        @CachedLibrary("container") DynamicObjectLibrary objectLibrary) {
    if (cachedFunction == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();

      NFunctionRootNode function = new NFunctionRootNode(language, getFrameDescriptor(), getBody(), getName());
      cachedFunction = new FunctionObject(rootShape, functionObject, getName(), function.getCallTarget(), getArgumentCount(), getIsVariadic() == 1);
    }

    objectLibrary.putConstant(container, getName(), cachedFunction, 0);
    return BladeNil.SINGLETON;
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    return false;
  }

  @Override
  public SourceSection getSourceSection() {
    // we want to disable debuggers from reaching function declarations
    return null;
  }
}
