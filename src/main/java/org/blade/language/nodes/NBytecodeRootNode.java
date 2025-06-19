package org.blade.language.nodes;

import com.oracle.truffle.api.bytecode.*;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.expressions.NAssignGlobalNode;
import org.blade.language.nodes.expressions.NGetGlobalNode;
import org.blade.language.nodes.expressions.arithemetic.*;
import org.blade.language.nodes.expressions.bitwise.*;
import org.blade.language.nodes.expressions.logical.*;
import org.blade.language.nodes.statements.NGlobalDeclNode;
import org.blade.language.nodes.util.NToBooleanNode;
import org.blade.language.nodes.util.NUnboxNode;
import org.blade.language.runtime.BladeNil;

@GenerateBytecode(
  languageClass = BladeLanguage.class,
  boxingEliminationTypes = {boolean.class, long.class},
  enableUncachedInterpreter = true,
  enableRootBodyTagging = false,
  enableSerialization = true,
  enableTagInstrumentation = true
)
@TypeSystemReference(BladeTypes.class)
@OperationProxy(NAddNode.class)
@OperationProxy(NDivideNode.class)
@OperationProxy(NFloorDivideNode.class)
@OperationProxy(NModuloNode.class)
@OperationProxy(NMultiplyNode.class)
@OperationProxy(NNegateNode.class)
@OperationProxy(NPowNode.class)
@OperationProxy(NSubtractNode.class)
@OperationProxy(NBitAndNode.class)
@OperationProxy(NBitLeftShiftNode.class)
@OperationProxy(NBitNotNode.class)
@OperationProxy(NBitOrNode.class)
@OperationProxy(NBitRightShiftNode.class)
@OperationProxy(NBitUnsignedRightShiftNode.class)
@OperationProxy(NBitXorNode.class)
@OperationProxy(NEqualNode.class)
@OperationProxy(NGreaterThanNode.class)
@OperationProxy(NGreaterThanOrEqualNode.class)
@OperationProxy(NLessThanNode.class)
@OperationProxy(NLessThanOrEqualNode.class)
@OperationProxy(NLogicalNotNode.class)
@OperationProxy(NNotEqualNode.class)
@OperationProxy(NUnboxNode.class)
@ShortCircuitOperation(name = "NAnd", booleanConverter = NToBooleanNode.class, operator = ShortCircuitOperation.Operator.AND_RETURN_CONVERTED)
@ShortCircuitOperation(name = "NOr", booleanConverter = NToBooleanNode.class, operator = ShortCircuitOperation.Operator.OR_RETURN_CONVERTED)
public abstract class NBytecodeRootNode extends RootNode implements BytecodeRootNode {
  public String name;
  public int parametersCount = 0;
  protected SourceSection sourceSection;

  public NBytecodeRootNode(BladeLanguage language, FrameDescriptor frameDescriptor) {
    super(language, frameDescriptor);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setParametersCount(int parametersCount) {
    this.parametersCount = parametersCount;
  }

  public void setSourceSection(SourceSection sourceSection) {
    this.sourceSection = sourceSection;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SourceSection getSourceSection() {
    return sourceSection;
  }

  @Operation(tags = DebuggerTags.AlwaysHalt.class)
  public static final class NAlwaysHalt {

    @Specialization
    static void doDefault() {
      // nothing to do. always the tag will trigger halt.
    }
  }

  @Operation
  @ConstantOperand(type = int.class)
  public static final class NLoadArgument {

    @Specialization(guards = "index < arguments.length")
    @ForceQuickening
    static Object doLoadInBounds(@SuppressWarnings("unused") VirtualFrame frame, int index,
                                 @Bind("frame.getArguments()") Object[] arguments) {
      return arguments[index];
    }

    @Fallback
    static Object doLoadOutOfBounds(@SuppressWarnings("unused") int index) {
      return BladeNil.SINGLETON;
    }

  }

  @Operation
  @ConstantOperand(type = TruffleString.class)
  @ConstantOperand(type = boolean.class)
  public static final class NSetGlobal {
    @Specialization
    public static void perform(VirtualFrame frame, TruffleString name, boolean isConstant, Object value,
                               @Bind Node node,
                               @Cached NGlobalScopeObjectNode globalScope,
                               @Cached NGlobalDeclNode declareNode) {
      declareNode.executeGlobal(node, globalScope.execute(frame), name, value, isConstant);
    }
  }

  @Operation
  @ConstantOperand(type = TruffleString.class)
  public static final class NGetGlobal {
    @Specialization
    public static Object perform(VirtualFrame frame, TruffleString name,
                               @Bind Node node,
                               @Cached NGlobalScopeObjectNode globalScope,
                               @Cached NGetGlobalNode getNode) {
      return getNode.executeGet(node, globalScope.execute(frame), name);
    }
  }

  @Operation
  @ConstantOperand(type = TruffleString.class)
  public static final class NUpdateGlobal {
    @Specialization
    public static Object perform(VirtualFrame frame, TruffleString name, Object value,
                               @Bind Node node,
                               @Cached NGlobalScopeObjectNode globalScope,
                               @Cached NAssignGlobalNode assignNode) {
      return assignNode.executeSet(node, globalScope.execute(frame), name, value);
    }
  }

  @Override
  public SourceSection ensureSourceSection() {
    return BytecodeRootNode.super.ensureSourceSection();
  }
}
