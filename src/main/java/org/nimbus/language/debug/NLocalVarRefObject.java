package org.nimbus.language.debug;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.source.SourceSection;
import org.nimbus.language.runtime.NimNil;

public final class NLocalVarRefObject extends NRefObject {
  private final int slot;

  public NLocalVarRefObject(String name, SourceSection sourceSection, int slot) {
    super(name, sourceSection);
    this.slot = slot;
  }

  @Override
  public Object read(Frame frame) {
    Object result = frame.getValue(slot);
    return result == null ? NimNil.SINGLETON : result;
  }

  @Override
  public void write(Frame frame, Object value) {
    frame.setObject(slot, value);
  }
}
