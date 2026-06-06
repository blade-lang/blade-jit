package org.zuri.language.nodes.common;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

@GenerateInline
@GenerateCached(false)
public abstract class NNormalizeIndexNode extends Node {
  @Specialization
  static int normalizeIndex(Node node, int index, int length,
                            @Cached @Cached.Shared("negativeIndexProfile") InlinedConditionProfile negativeIndexProfile,
                            @Cached @Cached.Shared("overflowProfile") InlinedConditionProfile overflowProfile) {
    if (negativeIndexProfile.profile(node, index < 0)) {
      return index + length;
    } else if (overflowProfile.profile(node, index > length)) {
      return length;
    }

    return index;
  }

  @Specialization
  static int normalizeIndex(Node node, long index, int length,
                            @Cached @Cached.Shared("negativeIndexProfile") InlinedConditionProfile negativeIndexProfile,
                            @Cached @Cached.Shared("overflowProfile") InlinedConditionProfile overflowProfile) {
    if (negativeIndexProfile.profile(node, index < 0)) {
      return (int) index + length;
    } else if (overflowProfile.profile(node, index > length)) {
      return length;
    }

    return (int) index;
  }

  public abstract int execute(Node node, int index, int length);

  public abstract int executeLong(Node node, long index, int length);
}
