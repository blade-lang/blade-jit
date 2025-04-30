package org.nimbus.language.debug;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.source.SourceSection;

import java.util.Objects;

public final class NFunctionArgRefObject extends NRefObject {
  private final int index;

  public NFunctionArgRefObject(String name, SourceSection sourceSection, int index) {
    super(name, sourceSection);
    this.index = index;
  }

  @Override
  public Object read(Frame frame) {
    return frame.getArguments()[index];
  }

  @Override
  public void write(Frame frame, Object value) {
    frame.getArguments()[index] = value;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof NFunctionArgRefObject ref) {
      return index == ref.index;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(index);
  }
}
