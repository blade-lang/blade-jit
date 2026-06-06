package org.zuri.language.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.runtime.ZuriNil;
import org.zuri.language.runtime.ListObject;

@TypeSystemReference(ZuriTypes.class)
public abstract class NNode extends NBaseNode {
  private SourceSection sourceSection = null;

  public static boolean evaluateBoolean(Object value) {
    if (value == ZuriNil.SINGLETON) {
      return false;
    } else if (value instanceof Boolean b) {
      return b;
    }

    // a number is falsy when it's 0
    if (value instanceof Long v) {
      return v != 0L;
    } else if (value instanceof Double d) {
      return d != 0D;
    }

    // handle objects
    if (value instanceof TruffleString string) {
      return !string.isEmpty();
    } else if (value instanceof ListObject list) {
      return list.getArraySize() != 0L;
    }

    return true;
  }

  @ExplodeLoop
  public static MaterializedFrame getParentFrame(VirtualFrame frame, int depth) {
    MaterializedFrame parentFrame = (MaterializedFrame) frame.getValue(0);
    while (depth-- > 1) {
      parentFrame = (MaterializedFrame) parentFrame.getValue(0);
    }
    return parentFrame;
  }

  public abstract Object execute(VirtualFrame frame);

  public boolean executeBoolean(VirtualFrame frame) {
    return evaluateBoolean(execute(frame));
  }

  public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
    return ZuriTypesGen.expectLong(execute(frame));
  }

  public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
    return ZuriTypesGen.expectDouble(execute(frame));
  }

  public Object evaluateReceiver(VirtualFrame frame) {
    return ZuriNil.SINGLETON;
  }

  public Object evaluateFunction(VirtualFrame frame, Object receiver) {
    return execute(frame);
  }

  @Override
  public SourceSection getSourceSection() {
    return sourceSection;
  }

  public NNode setSourceSection(SourceSection sourceSection) {
    this.sourceSection = sourceSection;
    return this;
  }
}
