package org.nimbus.language.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NimContext;
import org.nimbus.language.runtime.NimNil;

@TypeSystemReference(NimTypes.class)
public abstract class NNode extends NBaseNode {
  private SourceSection sourceSection = null;

  public abstract Object execute(VirtualFrame frame);

  public boolean executeBoolean(VirtualFrame frame) {
    return evaluateBoolean(execute(frame));
  }

  public long executeInt(VirtualFrame frame) throws UnexpectedResultException {
    return NimTypesGen.expectInteger(execute(frame));
  }

  public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
    return NimTypesGen.expectDouble(execute(frame));
  }

  public Object evaluateReceiver(VirtualFrame frame) {
    return NimNil.SINGLETON;
  }

  public Object evaluateFunction(VirtualFrame frame, Object receiver) {
    return execute(frame);
  }

  public boolean evaluateBoolean(Object value) {
    if (value == NimNil.SINGLETON) {
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
    if(value instanceof TruffleString string) {
      return !string.isEmpty();
    } else if(value instanceof NListObject list) {
      return list.getArraySize() != 0L;
    }

    // TODO: Handle dictionaries here...

    return true;
  }

  public NNode setSourceSection(SourceSection sourceSection) {
    this.sourceSection = sourceSection;
    return this;
  }

  @Override
  public SourceSection getSourceSection() {
    return sourceSection;
  }
}
