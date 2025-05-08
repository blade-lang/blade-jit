package org.blade.language.translator;

import org.blade.language.runtime.BladeClass;

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
    public final BladeClass object;

    public ClassObject(BladeClass object) {
      this.object = object;
    }
  }
}
