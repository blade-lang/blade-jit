package org.nimbus.language.translator;

import org.nimbus.language.runtime.NimClass;

public abstract class NFrameMember {

  public static final class FunctionArgument extends NFrameMember {
    public final int index;

    public FunctionArgument(int index) {
      this.index = index;
    }
  }

  public static final class LocalVariable extends NFrameMember {
    public final int index;
    public final boolean constant;

    public LocalVariable(int index, boolean constant) {
      this.index = index;
      this.constant = constant;
    }
  }

  public static final class ClassObject extends NFrameMember {
    public final NimClass object;

    public ClassObject(NimClass object) {
      this.object = object;
    }
  }
}
