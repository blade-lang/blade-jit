package org.zuri.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.zuri.language.nodes.NNode;
import org.zuri.language.nodes.common.NNormalizeIndexNode;
import org.zuri.language.runtime.*;
import org.zuri.language.shared.ZuriUtil;
import org.zuri.language.shared.BuiltinClassesModel;

@NodeChild("targetExpr")
@NodeChild("lowerExpr")
@NodeChild("upperExpr")
@ImportStatic(ZString.class)
public abstract class NGetSliceNode extends NNode {

  @Specialization(guards = {"string.isEmpty()"})
  protected Object doString(TruffleString string, long lower, long upper,
                            @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                            @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode) {
    return ZString.EMPTY;
  }

  @Specialization(guards = {"lower == upper"})
  protected Object doString2(TruffleString string, long lower, long upper,
                             @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                             @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode) {
    return ZString.EMPTY;
  }

  @Specialization(guards = {"!string.isEmpty()", "lower != upper"})
  protected Object doString3(TruffleString string, long lower, long upper,
                             @Cached @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                             @Cached @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                             @Cached @Cached.Shared("normalizeIndexNode") NNormalizeIndexNode normalizeIndexNode) {
    final int length = ZString.intLength(string, lengthNode);
    final int effectiveLower = normalizeIndexNode.executeLong(this, lower, length);
    final int effectiveUpper = normalizeIndexNode.executeLong(this, upper, length);

    if (effectiveUpper < effectiveLower) {
      return ZString.EMPTY;
    }

    return ZString.substring(string, effectiveLower, effectiveUpper, substringNode);
  }

  @Specialization(guards = {"list.getArraySize() == 0"})
  protected Object doList(ListObject list, long lower, long upper,
                          @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
                          @Cached("classesModel.listShape") Shape listShape,
                          @Cached("classesModel.listObject") ZuriClass listObject) {
    return new ListObject(listShape, listObject, new Object[0]);
  }

  @Specialization(guards = {"lower == upper"})
  protected Object doList2(ListObject list, long lower, long upper,
                           @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
                           @Cached("classesModel.listShape") Shape listShape,
                           @Cached("classesModel.listObject") ZuriClass listObject) {
    return new ListObject(listShape, listObject, new Object[0]);
  }

  @Specialization(guards = {"list.getArraySize() > 0", "lower != upper"})
  protected Object doList3(ListObject list, long lower, long upper,
                           @Bind Node node,
                           @Cached(value = "languageContext().objectsModel", neverDefault = true) @Cached.Shared("classesModel") BuiltinClassesModel classesModel,
                           @Cached @Cached.Shared("normalizeIndexNode") NNormalizeIndexNode normalizeIndexNode,
                           @Cached("classesModel.listShape") Shape listShape,
                           @Cached("classesModel.listObject") ZuriClass listObject) {
    final int length = (int)list.getArraySize();
    final int effectiveLower = normalizeIndexNode.executeLong(node, lower, length);
    final int effectiveUpper = normalizeIndexNode.executeLong(node, upper, length);

    if (effectiveUpper < effectiveLower) {
      return new ListObject(listShape, listObject, new Object[0]);
    }

    return list.getSlice(effectiveLower, effectiveUpper);
  }

  @Fallback
  protected Object doFallback(Object object, Object lower, Object upper) {
    throw ZuriRuntimeError.error(
      this,
      "Object of type ",
      ZuriUtil.getObjectType(object),
      " does not support slice operations"
    );
  }
}
