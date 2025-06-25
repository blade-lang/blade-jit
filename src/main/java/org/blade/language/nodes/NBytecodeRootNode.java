package org.blade.language.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.*;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.expressions.NAssignGlobalNode;
import org.blade.language.nodes.expressions.NGetGlobalNode;
import org.blade.language.nodes.expressions.NGetSliceNode;
import org.blade.language.nodes.expressions.arithemetic.*;
import org.blade.language.nodes.expressions.bitwise.*;
import org.blade.language.nodes.expressions.logical.*;
import org.blade.language.nodes.functions.NBuiltinFunctionNode;
import org.blade.language.nodes.functions.NDefCallNode;
import org.blade.language.nodes.list.NReadListIndexNode;
import org.blade.language.nodes.list.NWriteIndexNode;
import org.blade.language.nodes.statements.NGlobalDeclNode;
import org.blade.language.nodes.string.NReadStringPropertyNode;
import org.blade.language.nodes.util.NToBooleanNode;
import org.blade.language.nodes.util.NUnboxNode;
import org.blade.language.runtime.*;

@GenerateBytecode(
  languageClass = BladeLanguage.class,
  boxingEliminationTypes = {boolean.class, long.class},
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
  public TruffleString name;
  public int parametersCount = 0;
  protected SourceSection sourceSection;

  public NBytecodeRootNode(BladeLanguage language, FrameDescriptor frameDescriptor) {
    super(language, frameDescriptor);
  }

  public void setParametersCount(int parametersCount) {
    this.parametersCount = parametersCount;
  }

  @Override
  public String getName() {
    return name.toJavaStringUncached();
  }

  public void setName(TruffleString name) {
    this.name = name;
  }

  @Override
  public SourceSection getSourceSection() {
    return sourceSection;
  }

  public void setSourceSection(SourceSection sourceSection) {
    this.sourceSection = sourceSection;
  }

  @Override
  public SourceSection ensureSourceSection() {
    return BytecodeRootNode.super.ensureSourceSection();
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
                               @Cached(inline = true) NGlobalDeclNode declareNode) {
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
                                 @Cached(inline = true) NGetGlobalNode getNode) {
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
                                 @Cached(inline = true) NAssignGlobalNode assignNode) {
      return assignNode.executeSet(node, globalScope.execute(frame), name, value);
    }
  }

  @Operation
  public static final class NEcho {
    @Specialization
    public static Object doDouble(Object value, @Bind Node node) {
      BladeContext.get(node).println(value);
      return BladeNil.SINGLETON;
    }
  }

  @Operation
  @ImportStatic(BladeContext.class)
  public static final class NCreateList {
    @Specialization
    static Object doAny(@Variadic Object[] items,
                               @Bind Node node,
                               @Cached(value = "get(node)", neverDefault = true) BladeContext context,
                               @Cached(value = "context.objectsModel.listShape", neverDefault = true) Shape listShape,
                               @Cached(value = "context.objectsModel.listObject", neverDefault = true) BladeClass listClass) {
      return new ListObject(listShape, listClass, items);
    }
  }

  @Operation
  @ImportStatic(BladeContext.class)
  public static final class NCreateRange {
    @Specialization
    static Object doAny(long lower, long upper, @Bind Node node,
                               @Cached(value = "get(node)", neverDefault = true) BladeContext context,
                               @Cached(value = "context.objectsModel.rootShape", neverDefault = true) Shape rootShape,
                               @Cached(value = "context.objectsModel.rangeObject", neverDefault = true) BladeClass rangeClass) {
      return new RangeObject(rootShape, rangeClass, lower, upper);
    }

    @Fallback
    static Object doUnsupported(Object lower, Object upper, @Bind Node node) {
      throw BladeRuntimeError.argumentError(node, "..", lower, upper);
    }
  }

  @Operation
  public static final class NGetIndex {
    @Specialization
    public static Object doListLong(ListObject list, Object index, @Bind Node node, @Cached(inline = true) NReadListIndexNode readNode) {
      return readNode.executeRead(node, list, index);
    }

    @Specialization
    public static Object doString(TruffleString list, Object index, @Cached NReadStringPropertyNode readNode) {
      return readNode.executeProperty(list, index);
    }

    @Fallback
    public static Object doOthers(Object list, Object index, @Cached NSharedPropertyReaderNode readNode) {
      return readNode.executeRead(list, index);
    }
  }

  @Operation
  public static final class NSetIndex {
    @Specialization
    public static Object perform(Object list, Object index, Object value, @Bind Node node, @Cached(inline = true) NWriteIndexNode readNode) {
      return readNode.executeWrite(node, list, index, value);
    }
  }

  @Operation
  public static final class NGetSlice {
    @Specialization
    public static Object doListLong(ListObject list, Object lower, Object upper, @Bind Node node, @Cached(inline = true) NGetSliceNode getSliceNode) {
      return getSliceNode.executeSlice(node, list, lower, upper);
    }
  }

  @Operation
  @ConstantOperand(type = TruffleString.class)
  public static final class NGetProperty {
    @Specialization
    public static Object perform(TruffleString name, Object object,
                                 @Cached NSharedPropertyReaderNode propertyReaderNode) {
      Object result = propertyReaderNode.executeRead(object, name);

      if(result instanceof BoundFunctionObj boundFunction) {
        boundFunction.setInstance(object);
      }

      return result;
    }
  }

  @Operation
  @ConstantOperand(type = TruffleString.class)
  public static final class NSetProperty {
    @Specialization
    public static Object perform(TruffleString name, Object object, Object value,
                                 @Cached NSharedPropertyWriterNode propertyWriterNode) {
      return propertyWriterNode.executeWrite(object, name.toJavaStringUncached(), value);
    }
  }

  @Operation
  @ConstantOperand(type = int.class)
  public static final class NDefCall {
    @Specialization(guards = "!isNull(boundFunction.getInstance())")
    public static Object doValidBoundFunction(int argumentsLength, BoundFunctionObj boundFunction, @Variadic Object[] arguments, @Bind Node node,
                                    @Cached(inline = true) @Cached.Shared("defCallNode") NDefCallNode defCallNode) {
      return defCallNode.executeCall(node, argumentsLength + 1, boundFunction, boundArguments(argumentsLength, boundFunction.getInstance(), arguments));
    }

    @Specialization(guards = "isNull(boundFunction.getInstance())")
    public static Object doInvalidBoundFunction(int argumentsLength, BoundFunctionObj boundFunction, @Variadic Object[] arguments,
                                    @Bind Node node) {
      throw BladeRuntimeError.error(node, "Invalid bound function");
    }

    @Specialization
    public static Object doAll(int argumentsLength, Object function, @Variadic Object[] arguments, @Bind Node node,
                                    @Cached(inline = true) @Cached.Shared("defCallNode") NDefCallNode defCallNode) {
      return defCallNode.executeCall(node, argumentsLength, function, arguments);
    }

    static boolean isNull(Object v) {
      return v == null;
    }

    static Object[] boundArguments(int length, Object value, Object[] arguments) {
      final int argumentsLength = length + 1;
      Object[] boundArguments = new Object[argumentsLength];
      boundArguments[0] = value;

      System.arraycopy(arguments, 0, boundArguments, 1, length);

      return boundArguments;
    }
  }

  @Operation
  @ConstantOperand(type = NodeFactory.class)
  @ConstantOperand(type = int.class)
  @ConstantOperand(type = boolean.class)
  public static final class NBuiltin {

    @Specialization(guards = {"arguments.length == argumentCount"})
    @SuppressWarnings("unused")
    static Object doInBounds(VirtualFrame frame,
                             NodeFactory<?> factory,
                             int argumentCount,
                             boolean isVariadic,
                             @Bind Node bytecode,
                             @Bind("frame.getArguments()") Object[] arguments,
                             @Cached.Shared @Cached(value = "createBuiltin(factory)", uncached = "getUncachedBuiltin()", neverDefault = true) NBuiltinFunctionNode builtin) {
      return doInvoke(frame, bytecode, builtin, arguments);
    }

    @Fallback
    @SuppressWarnings("unused")
    static Object doOutOfBounds(VirtualFrame frame,
                                NodeFactory<?> factory,
                                int argumentCount,
                                boolean isVariadic,
                                @Bind Node bytecode,
                                @Cached.Shared @Cached(value = "createBuiltin(factory)", uncached = "getUncachedBuiltin()", neverDefault = true) NBuiltinFunctionNode builtin) {
      Object[] originalArguments = frame.getArguments();
      Object[] arguments = new Object[argumentCount];
      for (int i = 0; i < argumentCount; i++) {
        if (i < originalArguments.length) {
          arguments[i] = originalArguments[i];
        } else {
          arguments[i] = BladeNil.SINGLETON;
        }
      }
      return doInvoke(frame, bytecode, builtin, arguments);
    }

    static NBuiltinFunctionNode createBuiltin(NodeFactory<?> factory) {
      return (NBuiltinFunctionNode) factory.createNode();
    }

    static NBuiltinFunctionNode getUncachedBuiltin() {
      throw CompilerDirectives.shouldNotReachHere("Builtins should not execute uncached.");
    }

    private static Object doInvoke(VirtualFrame frame, Node node, NBuiltinFunctionNode builtin, Object[] arguments) {
      try {
        if (builtin.getParent() == null) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          node.insert(builtin);
        }

        return builtin.execute(frame, arguments);
      } catch (UnsupportedSpecializationException e) {
        throw BladeRuntimeError.typeError(e.getNode(), "", e.getSuppliedValues());
      }
    }
  }

  /*@Operation
  public static final class NDefCallNode {
    @Specialization(
      limit = "3",
      guards = "function.getCallTarget() == cachedTarget",
      assumptions = "callTargetStable"
    )
    static Object doDirect(FunctionObj function, @Variadic Object[] arguments,
                                     @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                     @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                     @Cached("create(cachedTarget)") DirectCallNode callNode) {
      *//* Inline cache hit, we are safe to execute the cached call target. *//*
      return callNode.call(extendArguments(arguments, function.getArgumentsCount()));
    }

    @Specialization(replaces = "doDirect")
    static Object doIndirect(FunctionObj function, @Variadic Object[] arguments,
                                       @Cached IndirectCallNode callNode) {
      return callNode.call(function.getCallTarget(), extendArguments(arguments, function.getArgumentsCount()));
    }

    @Specialization
    static Object doInterop(Object function, @Variadic Object[] arguments,
                                      @Bind Node node,
                                      @CachedLibrary(limit = "3") InteropLibrary library) {
      try {
        return library.execute(function, arguments);
      } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
        *//* Execute was not successful. *//*
        throw BladeRuntimeError.error(node, "'", function, "' is not a callable function");
      }
    }

    @ExplodeLoop
    private static Object[] extendArguments(Object[] arguments, int finalLength) {
      int argumentLength = arguments.length;

      Object[] ret = new Object[finalLength];

      if (argumentLength > 0) {
        System.arraycopy(arguments, 1, ret, 1, argumentLength - 1);
      }

      for (int i = argumentLength; i < finalLength; i++) {
        ret[i] = BladeNil.SINGLETON;
      }

      return ret;
    }
  }*/
}
