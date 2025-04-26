package org.nimbus.language.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.nodes.functions.NReadFunctionArgsExprNode;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NimContext;
import org.nimbus.language.runtime.NimLanguageView;
import org.nimbus.language.runtime.NimNil;

import java.io.PrintWriter;
import java.util.List;

@SuppressWarnings({"javadoc", "unused"})
public final class PrintBuiltinFunctionNodeFactory implements NodeFactory<PrintBuiltinFunctionNodeFactory.PrintBuiltinFunctionNode> {

  private static final PrintBuiltinFunctionNodeFactory INSTANCE = new PrintBuiltinFunctionNodeFactory();

  @Override
  public PrintBuiltinFunctionNode createNode(Object... arguments) {
    if (arguments.length == 1 && (arguments[0] == null || arguments[0] instanceof NReadFunctionArgsExprNode[])) {
      return create((NReadFunctionArgsExprNode[]) arguments[0]);
    } else {
      throw new IllegalArgumentException("Invalid create signature.");
    }
  }

  @Override
  public Class<PrintBuiltinFunctionNode> getNodeClass() {
    return PrintBuiltinFunctionNode.class;
  }

  @Override
  public List<List<Class<?>>> getNodeSignatures() {
    return List.of(List.of(NReadFunctionArgsExprNode[].class));
  }

  @Override
  public List<Class<? extends Node>> getExecutionSignature() {
    return List.of();
  }

  @NeverDefault
  public static PrintBuiltinFunctionNode create(NReadFunctionArgsExprNode[] arguments) {
    return new PrintBuiltinFunctionNode(arguments);
  }

  public static NodeFactory<PrintBuiltinFunctionNode> getInstance() {
    return INSTANCE;
  }

  public static final class PrintBuiltinFunctionNode extends NBuiltinFunctionNode {

    private final InteropLibrary UNCACHED_LIB = InteropLibrary.getUncached();

    private PrintBuiltinFunctionNode(NReadFunctionArgsExprNode[] arguments) {}

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
      Object[] arguments = ((NListObject) frame.getArguments()[0]).items;
      NimContext context = NimContext.get(this);

      int length = arguments.length;

      for (int i = 1; i < length - 1; i++) {
        if (arguments[i] != NimNil.SINGLETON) {
          print(context.output, arguments[i]);
          print(context.output, " ");
        }
      }

      if (arguments[length - 1] != NimNil.SINGLETON) {
        print(context.output, arguments[length - 1]);
      }

      context.flushOutput();
      return NimNil.SINGLETON;
    }

    @CompilerDirectives.TruffleBoundary
    private void print(PrintWriter writer, Object value) {
      writer.print(UNCACHED_LIB.toDisplayString(NimLanguageView.forValue(value)));
    }
  }
}
