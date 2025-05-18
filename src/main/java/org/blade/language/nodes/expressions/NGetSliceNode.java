package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NNormalizeIndexNode;
import org.blade.language.runtime.*;
import org.blade.language.shared.BladeUtil;
import org.blade.language.shared.BuiltinClassesModel;

@NodeChild("targetExpr")
@NodeChild("lowerExpr")
@NodeChild("upperExpr")
@ImportStatic(BString.class)
public abstract class NGetSliceNode extends NNode {

  @Specialization(guards = {"intLength(string, lengthNode) == 0"})
  protected Object doString(TruffleString string, long lower, long upper,
                            @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                            @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode) {
    return BString.EMPTY;
  }

  @Specialization(guards = {"lower == upper"})
  protected Object doString2(TruffleString string, long lower, long upper,
                             @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                             @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode) {
    return BString.EMPTY;
  }

  @Specialization(guards = {"intLength(string, lengthNode) > 0", "lower != upper"})
  protected Object doString3(TruffleString string, long lower, long upper,
                             @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                             @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                             @Cached @Cached.Shared("normalizeIndexNode") NNormalizeIndexNode normalizeIndexNode) {
    final int length = BString.intLength(string, lengthNode);
    final int effectiveLower = normalizeIndexNode.executeLong(this, lower, length);
    final int effectiveUpper = normalizeIndexNode.executeLong(this, upper, length);

    if (effectiveUpper < effectiveLower) {
      return BString.EMPTY;
    }

    return BString.substring(string, effectiveLower, effectiveUpper, substringNode);
  }

  @Specialization(guards = {"items.length == 0"})
  protected Object doList(ListObject list, long lower, long upper,
                          @Cached(value = "list.items", dimensions = 1) Object[] items,
                          @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
                          @Cached("classesModel.listShape") Shape listShape,
                          @Cached("classesModel.listObject") BladeClass listObject) {
    return new ListObject(listShape, listObject, new Object[0]);
  }

  @Specialization(guards = {"lower == upper"})
  protected Object doList2(ListObject list, long lower, long upper,
                           @Cached(value = "list.items", dimensions = 1) Object[] items,
                           @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
                           @Cached("classesModel.listShape") Shape listShape,
                           @Cached("classesModel.listObject") BladeClass listObject) {
    return new ListObject(listShape, listObject, new Object[0]);
  }

  @ExplodeLoop
  @Specialization(guards = {"items.length > 0", "lower != upper"})
  protected Object doList3(ListObject list, long lower, long upper,
                           @Bind Node node,
                           @Cached(value = "list.items", dimensions = 1) Object[] items,
                           @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
                           @Cached @Cached.Shared("normalizeIndexNode") NNormalizeIndexNode normalizeIndexNode,
                           @Cached("classesModel.listShape") Shape listShape,
                           @Cached("classesModel.listObject") BladeClass listObject) {
    final int length = items.length;
    final int effectiveLower = normalizeIndexNode.executeLong(node, lower, length);
    final int effectiveUpper = normalizeIndexNode.executeLong(node, upper, length);

    if (effectiveUpper < effectiveLower) {
      return new ListObject(listShape, listObject, new Object[0]);
    }

    int effectiveLength = effectiveUpper - effectiveLower;
    Object[] objects = new Object[effectiveLength];
    System.arraycopy(items, effectiveLower, objects, 0, effectiveLength);

    return new ListObject(listShape, listObject, objects);
  }

  @Fallback
  protected Object doFallback(Object object, Object lower, Object upper) {
    throw BladeRuntimeError.error(this, "Object of type ", BladeUtil.getObjectType(object), " does not support slice operations");
  }
}
