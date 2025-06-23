package org.blade.language.nodes.literals;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.list.NReadListIndexNode;
import org.blade.language.nodes.list.NReadListIndexNodeGen;
import org.blade.language.runtime.BladeClass;
import org.blade.language.runtime.BladeContext;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.RangeObject;
import org.blade.language.shared.BuiltinClassesModel;

@NodeChild("lower")
@NodeChild("upper")
@ImportStatic(BladeContext.class)
public abstract class NRangeLiteralNode extends NNode {
  public abstract Object executeRead(Object list, Object index);

  @Specialization
  protected Object doValid(long lower, long upper, @Bind Node node,
                           @Cached("get(node)") BladeContext context,
                           @Cached("context.objectsModel.rootShape")Shape rootShape,
                           @Cached("context.objectsModel.rangeObject") BladeClass rangeClass) {
    return new RangeObject(rootShape, rangeClass, lower, upper);
  }

  @Fallback
  protected Object doUnsupported(Object lower, Object upper) {
    throw BladeRuntimeError.argumentError(this, "..", lower, upper);
  }
}
