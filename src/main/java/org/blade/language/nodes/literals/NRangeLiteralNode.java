package org.blade.language.nodes.literals;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeClass;
import org.blade.language.runtime.BladeContext;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.RangeObject;

@NodeChild("lower")
@NodeChild("upper")
@ImportStatic(BladeContext.class)
public abstract class NRangeLiteralNode extends NNode {

  @Specialization
  protected static Object doValid(long lower, long upper, @Bind Node node,
                                  @Cached(value = "get(node)", neverDefault = true) BladeContext context,
                                  @Cached(value = "context.objectsModel.rootShape", neverDefault = true) Shape rootShape,
                                  @Cached(value = "context.objectsModel.rangeObject", neverDefault = true) BladeClass rangeClass) {
    return new RangeObject(rootShape, rangeClass, lower, upper);
  }

  @Fallback
  protected static Object doUnsupported(Object lower, Object upper, @Bind Node node) {
    throw BladeRuntimeError.argumentError(node, "..", lower, upper);
  }
}
