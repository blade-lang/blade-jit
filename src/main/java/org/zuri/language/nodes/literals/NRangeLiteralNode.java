package org.zuri.language.nodes.literals;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import org.zuri.language.nodes.NNode;
import org.zuri.language.runtime.ZuriClass;
import org.zuri.language.runtime.ZuriContext;
import org.zuri.language.runtime.ZuriRuntimeError;
import org.zuri.language.runtime.RangeObject;

@NodeChild("lower")
@NodeChild("upper")
@ImportStatic(ZuriContext.class)
public abstract class NRangeLiteralNode extends NNode {

  @Specialization
  protected static Object doValid(long lower, long upper, @Bind Node node,
                                  @Cached(value = "get(node)", neverDefault = true) ZuriContext context,
                                  @Cached(value = "context.objectsModel.rootShape", neverDefault = true) Shape rootShape,
                                  @Cached(value = "context.objectsModel.rangeObject", neverDefault = true) ZuriClass rangeClass) {
    return new RangeObject(rootShape, rangeClass, lower, upper);
  }

  @Fallback
  protected static Object doUnsupported(Object lower, Object upper, @Bind Node node) {
    throw ZuriRuntimeError.argumentError(node, "..", lower, upper);
  }
}
