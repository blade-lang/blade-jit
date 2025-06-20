package org.blade.language.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.bytecode.*;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.Shape;
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

  @Operation
  @ConstantOperand(type = int.class)
  @ImportStatic(BladeContext.class)
  public static final class NDefCallNode {

    @Specialization(guards = {"function.getArgumentsCount() == argumentsLength", "!function.isVariadic()"}, assumptions = "callTargetStable")
    public static Object doSameSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                    @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                    @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                    @Cached("create(cachedTarget)") DirectCallNode callNode) {
      return callNode.call(arguments);
    }

    @Specialization(guards = {"function.isVariadic()", "argumentsLength < function.getArgumentsCount()"}, assumptions = "callTargetStable")
    public static Object doVariableLessSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                            @Bind Node node,
                                            @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                            @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                            @Cached("create(cachedTarget)") DirectCallNode callNode,
                                            @Cached(value = "get(node)", uncached = "get(node)") BladeContext context,
                                            @Cached("context.objectsModel.listShape") Shape listShape,
                                            @Cached("context.objectsModel.listObject") BladeClass listClass) {
      return callNode.call(expandLessVarArguments(
        arguments,
        function.getArgumentsCount(),
        argumentsLength,
        listShape,
        listClass
      ));
    }

    @Specialization(guards = {"function.isVariadic()", "argumentsLength >= function.getArgumentsCount()", "function.getArgumentsCount() > 1"}, assumptions = "callTargetStable")
    public static Object doVariableMoreSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                            @Bind Node node,
                                            @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                            @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                            @Cached("create(cachedTarget)") DirectCallNode callNode,
                                            @Cached(value = "get(node)", uncached = "get(node)") BladeContext context,
                                            @Cached("context.objectsModel.listShape") Shape listShape,
                                            @Cached("context.objectsModel.listObject") BladeClass listClass) {
      return callNode.call(expandMoreVarArguments(
        arguments,
        function.getArgumentsCount(),
        argumentsLength,
        listShape,
        listClass
      ));
    }

    @Specialization(guards = {"function.isVariadic()", "arguments.length >= function.getArgumentsCount()", "function.getArgumentsCount() == 1"}, assumptions = "callTargetStable")
    public static Object doVariableNoSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                          @Bind Node node,
                                          @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                          @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                          @Cached("create(cachedTarget)") DirectCallNode callNode,
                                          @Cached(value = "get(node)", uncached = "get(node)") BladeContext context,
                                          @Cached("context.objectsModel.listShape") Shape listShape,
                                          @Cached("context.objectsModel.listObject") BladeClass listClass) {
      return callNode.call(expandNoVarArguments(arguments, argumentsLength, listShape, listClass));
    }

    @Specialization(replaces = "doSameSize")
    public static Object doNotSameSize(int argumentsLength, FunctionObj function, @Variadic Object[] arguments,
                                       @Cached("function.getCallTargetStable()") Assumption callTargetStable,
                                       @Cached("function.getCallTarget()") RootCallTarget cachedTarget,
                                       @Cached("create(cachedTarget)") DirectCallNode callNode) {
      return callNode.call(extendArguments(arguments, function.getArgumentsCount() + 1));
    }

    @Specialization
    public static Object doInterop(int argumentsLength, Object function, @Variadic Object[] arguments,
                                   @CachedLibrary(limit = "3") InteropLibrary library, @Bind Node node) {
      try {
        return library.execute(function, arguments);
      } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
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

    // Specially used for variadic functions
    @ExplodeLoop
    private static Object[] expandLessVarArguments(Object[] arguments, int functionArity, int argumentsLength, Shape listShape, BladeClass listClass) {
      int argumentLength = arguments.length;

      int finalLength = functionArity + 1;
      Object[] ret = new Object[finalLength];

      if (argumentLength > 0) {
        System.arraycopy(arguments, 1, ret, 1, argumentLength - 1);
      }

      for (int i = argumentLength; i < functionArity; i++) {
        ret[i] = BladeNil.SINGLETON;
      }

      ret[functionArity] = new ListObject(
        listShape,
        listClass,
        new Object[0]
      );

      return ret;
    }

    // Specially used for variadic functions
    @ExplodeLoop
    private static Object[] expandMoreVarArguments(Object[] arguments, int functionArity, int argumentsLength, Shape listShape, BladeClass listClass) {
      int finalLength = functionArity + 1;
      Object[] ret = new Object[finalLength];

      System.arraycopy(arguments, 1, ret, 1, functionArity - 1);

      int varLength = argumentsLength - functionArity;
      Object[] variadic = new Object[varLength];
      System.arraycopy(arguments, functionArity, variadic, 0, varLength);

      ret[functionArity] = new ListObject(
        listShape,
        listClass,
        variadic
      );

      return ret;
    }

    // Specially used for variadic functions
    @ExplodeLoop
    private static Object[] expandNoVarArguments(Object[] arguments, int argumentsLength, Shape listShape, BladeClass listClass) {
      Object[] ret = new Object[2];

      Object[] variadic = new Object[argumentsLength - 1];
      System.arraycopy(arguments, 1, variadic, 0, argumentsLength - 1);

      ret[1] = new ListObject(
        listShape,
        listClass,
        variadic
      );

      return ret;
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
