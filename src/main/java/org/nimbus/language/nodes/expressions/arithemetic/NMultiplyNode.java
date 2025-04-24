package org.nimbus.language.nodes.expressions.arithemetic;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NBinaryNode;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NimContext;
import org.nimbus.language.runtime.NimRuntimeError;
import org.nimbus.language.shared.NBuiltinClassesModel;

@ImportStatic(Integer.class)
public abstract class NMultiplyNode extends NBinaryNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long doLongs(long left, long right) {
    return Math.multiplyExact(left, right);
  }

  @Specialization(replaces = "doLongs")
  protected double doDoubles(double left, double right) {
    return left * right;
  }

  @Specialization
  protected TruffleString doStringMultiplication(TruffleString string, long count,
                                                 @Cached TruffleString.RepeatNode repeatNode) {
    return repeatNode.execute(string, (int)count, NimbusLanguage.ENCODING);
  }

  @Specialization(guards = "count <= MAX_VALUE")
  protected NListObject doListMultiplication(NListObject list, long count) {
    NBuiltinClassesModel objectModel = NimContext.get(this).objectsModel;
    return new NListObject(
      objectModel.listShape,
      objectModel.listObject,
      repeatList(list, count)
    );
  }

  @Specialization(guards = "count > MAX_VALUE")
  protected NListObject doListMultiplicationOutOfBound(NListObject list, long count) {
    throw NimRuntimeError.create("List multiplication count out of bounds (", count, " > ", Integer.MAX_VALUE, ")");
  }

  @ExplodeLoop
  private Object[] repeatList(NListObject list, long count) {
    int size = (int) list.getArraySize();
    int finalSize = (int)(size * count);

    Object[] objects = new Object[finalSize];

    for(int i = 0; i < count; i++) {
      System.arraycopy(list.items, 0, objects, i * size, size);
    }

    return objects;
  }

  @Fallback
  protected double doUnsupported(Object left, Object right) {
    throw NimRuntimeError.argumentError(this, "*", left, right);
  }
}
