package org.zuri.language.debug;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.source.SourceSection;
import org.zuri.language.runtime.ZuriNil;

public final class LocalVarRefObject extends RefObject {
  private final int slot;

  public LocalVarRefObject(String name, SourceSection sourceSection, int slot) {
    super(name, sourceSection);
    this.slot = slot;
  }

  @Override
  public Object read(Frame frame) {
    Object result = frame.getValue(slot);
    return result == null ? ZuriNil.SINGLETON : result;
  }

  @Override
  public void write(Frame frame, Object value) {
    frame.setObject(slot, value);
  }
}
