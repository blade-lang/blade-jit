package org.blade.language.nodes.functions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
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
@NodeField(name = "captures", type = int.class)
@ImportStatic(BladeLanguage.class)
public abstract class NFunctionStmtNode extends NStmtNode {
  @CompilerDirectives.CompilationFinal
  private FunctionObject cachedFunction = null;
  private MaterializedFrame materializedFrame = null;

  protected abstract String getName();

  protected abstract FrameDescriptor getFrameDescriptor();

  protected abstract NFunctionBodyNode getBody();

  protected abstract int getArgumentCount();

  protected abstract int getIsVariadic();

  @Idempotent
  protected abstract int getCaptures();

  @Specialization(limit = "3", guards = "getCaptures() == 0")
  public Object declareNoCapture(VirtualFrame frame, DynamicObject container,
                                 @Bind Node node,
                                 @Cached("get(node)") BladeLanguage language,
                                 @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
                                 @Cached("classesModel.functionObject") BladeClass functionObject,
                                 @Cached("classesModel.rootShape") Shape rootShape,
                                 @CachedLibrary("container") DynamicObjectLibrary objectLibrary) {
    if (cachedFunction == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();

      NFunctionRootNode function = new NFunctionRootNode(language, getFrameDescriptor(), getBody(), getName(), null);

      cachedFunction = new FunctionObject(
        rootShape, functionObject, getName(), function.getCallTarget(),
        getArgumentCount(), getIsVariadic() == 1
      );
    }

    objectLibrary.putConstant(container, getName(), cachedFunction, 0);
    return BladeNil.SINGLETON;
  }

  @Specialization(limit = "3", guards = "getCaptures() == 1")
  public Object declareWithCapture(VirtualFrame frame, DynamicObject container,
                                   @Bind Node node,
                                   @Cached("get(node)") BladeLanguage language,
                                   @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
                                   @Cached("classesModel.functionObject") BladeClass functionObject,
                                   @Cached("classesModel.rootShape") Shape rootShape,
                                   @CachedLibrary("container") DynamicObjectLibrary objectLibrary) {
    if (materializedFrame == null) {
      materializedFrame = frame.materialize();
    }

    pushArgumentsToFrameSlots(frame, materializedFrame);

    if (cachedFunction == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();

      NFunctionRootNode function = new NFunctionRootNode(
        language,
        getFrameDescriptor(),
        getBody(),
        getName(),
        materializedFrame
      );

      cachedFunction = new FunctionObject(
        rootShape, functionObject, getName(), function.getCallTarget(),
        getArgumentCount(), getIsVariadic() == 1
      );
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

  @NonIdempotent
  @ExplodeLoop
  private void pushArgumentsToFrameSlots(VirtualFrame frame, MaterializedFrame materializedFrame) {
    Object[] arguments = frame.getArguments();
    int length = arguments.length;

    for (int i = 0; i < length; i++) {
      int slot = i + 1;
      materializedFrame.setObject(slot, arguments[i]);
    }
  }
}
