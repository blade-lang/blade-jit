package org.nimbus.language.shared;

import java.util.Objects;

public final class NLocalRefSlot {
  public final String name;
  public final int index;

  public NLocalRefSlot(String name, int index) {
    this.name = name;
    this.index = index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, index);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof NLocalRefSlot that)) {
      return false;
    }
    return this.index == that.index &&
      this.name.equals(that.name);
  }

  @Override
  public String toString() {
    return this.name + "-" + this.index;
  }
}
