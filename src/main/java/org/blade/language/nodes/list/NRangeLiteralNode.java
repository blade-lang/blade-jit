package org.blade.language.nodes.list;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.BladeContext;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.ListObject;
import org.blade.language.runtime.RangeObject;
import org.blade.language.shared.BuiltinClassesModel;

import java.util.List;

@NodeChild("lower")
@NodeChild("upper")
public abstract class NRangeLiteralNode extends NNode {

  @Specialization
  protected Object doValid(long lower, long upper) {
    BuiltinClassesModel classesModel = BladeContext.get(this).objectsModel;
    return new RangeObject(classesModel.rootShape, classesModel.rangeObject, lower, upper);
  }

  @Fallback
  protected Object doUnsupported(Object lower, Object upper) {
    throw BladeRuntimeError.valueError(this, "Invalid range parameters");
  }
}
