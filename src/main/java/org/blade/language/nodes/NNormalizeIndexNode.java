package org.blade.language.nodes;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

@GenerateInline
@GenerateCached(false)
public abstract class NNormalizeIndexNode extends Node {
  public abstract int execute(Node node, int index, int length);
  public abstract int executeLong(Node node, long index, int length);

  @Specialization
  static int normalizeIndex(Node node, int index, int length,
                            @Cached @Cached.Shared("negativeIndexProfile") InlinedConditionProfile negativeIndexProfile,
                            @Cached @Cached.Shared("overflowProfile") InlinedConditionProfile overflowProfile) {
    if (negativeIndexProfile.profile(node, index < 0)) {
      return index + length;
    } else if(overflowProfile.profile(node, index > length)) {
      return length;
    }

    return index;
  }

  @Specialization
  static int normalizeIndex(Node node, long index, int length,
                            @Cached @Cached.Shared("negativeIndexProfile") InlinedConditionProfile negativeIndexProfile,
                            @Cached @Cached.Shared("overflowProfile") InlinedConditionProfile overflowProfile) {
    if (negativeIndexProfile.profile(node, index < 0)) {
      return (int)index + length;
    } else if(overflowProfile.profile(node, index > length)) {
      return length;
    }

    return (int)index;
  }
}
