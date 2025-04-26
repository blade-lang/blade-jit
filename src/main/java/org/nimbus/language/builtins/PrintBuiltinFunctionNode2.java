package org.nimbus.language.builtins;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import org.nimbus.language.nodes.functions.NBuiltinFunctionNode;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NimContext;
import org.nimbus.language.runtime.NimNil;

public abstract class PrintBuiltinFunctionNode2 extends NBuiltinFunctionNode {

  @Specialization
  public Object doList(NListObject object,
                       @CachedLibrary(limit = "3")InteropLibrary interopLibrary,
                       @Cached(value = "languageContext()", neverDefault = true)NimContext context) {
    print(context, interopLibrary, object.items);
    return NimNil.SINGLETON;
  }

  @Fallback
  protected Object fallback(Object object) {
    NimContext.get(this).println("Something not working right: " + object);
    return NimNil.SINGLETON;
  }

  @ExplodeLoop
  private void print(NimContext context, InteropLibrary interopLibrary, Object[] arguments) {
    int length = arguments.length;

    for (int i = 1; i < length - 1; i++) {
      if (arguments[i] != NimNil.SINGLETON) {
        context.print(interopLibrary.toDisplayString(arguments[i]));
        context.print(" ");
      }
    }

    if (arguments[length - 1] != NimNil.SINGLETON) {
      context.print(interopLibrary.toDisplayString(arguments[length - 1]));
    }

    context.flushOutput();
  }
}
