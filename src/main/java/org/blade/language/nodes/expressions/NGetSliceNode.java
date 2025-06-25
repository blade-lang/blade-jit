package org.blade.language.nodes.expressions;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NNormalizeIndexNode;
import org.blade.language.nodes.list.NReadListIndexNode;
import org.blade.language.nodes.list.NReadListIndexNodeGen;
import org.blade.language.runtime.*;
import org.blade.language.shared.BladeUtil;
import org.blade.language.shared.BuiltinClassesModel;

@GenerateInline
@ImportStatic({BString.class, BladeContext.class})
public abstract class NGetSliceNode extends Node {

  @NeverDefault
  public static NGetSliceNode create() {
    return NGetSliceNodeGen.create();
  }

  public abstract Object executeSlice(Node Node, Object object, Object lower, Object upper);

  @Specialization(guards = {"intLength(string, lengthNode) == 0"})
  protected static Object doString(Node node, TruffleString string, long lower, long upper,
                            @Cached(inline = false) @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                            @Cached(inline = false) @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode) {
    return BString.EMPTY;
  }

  @Specialization(guards = {"lower == upper"})
  protected static Object doString2(Node node, TruffleString string, long lower, long upper,
                             @Cached(inline = false) @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                             @Cached(inline = false) @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode) {
    return BString.EMPTY;
  }

  @Specialization(guards = {"intLength(string, lengthNode) > 0", "lower != upper"})
  protected static Object doString3(Node node, TruffleString string, long lower, long upper,
                             @Cached(inline = false) @Cached.Shared("lengthNode") TruffleString.CodePointLengthNode lengthNode,
                             @Cached(inline = false) @Cached.Shared("substringNode") TruffleString.SubstringNode substringNode,
                             @Cached @Cached.Shared("normalizeLowerIndexNode") NNormalizeIndexNode normalizeLowerIndexNode,
                             @Cached @Cached.Shared("normalizeUpperIndexNode") NNormalizeIndexNode normalizeUpperIndexNode) {
    final int length = BString.intLength(string, lengthNode);
    final int effectiveLower = normalizeLowerIndexNode.executeLong(node, lower, length);
    final int effectiveUpper = normalizeUpperIndexNode.executeLong(node, upper, length);

    if (effectiveUpper < effectiveLower) {
      return BString.EMPTY;
    }

    return BString.substring(string, effectiveLower, effectiveUpper, substringNode);
  }

  @Specialization(guards = {"items.length == 0"})
  protected static Object doList(Node node, ListObject list, long lower, long upper,
                          @Cached(value = "list.items", dimensions = 1) Object[] items,
                          @Cached(value = "get(node)") BladeContext context,
                          @Cached(value = "context.objectsModel") BuiltinClassesModel classesModel,
                          @Cached("classesModel.listShape") Shape listShape,
                          @Cached("classesModel.listObject") BladeClass listObject) {
    return new ListObject(listShape, listObject, new Object[0]);
  }

  @Specialization(guards = {"lower == upper"})
  protected static Object doList2(Node node, ListObject list, long lower, long upper,
                           @Cached(value = "list.items", dimensions = 1) Object[] items,
                           @Cached(value = "get(node)") BladeContext context,
                           @Cached(value = "context.objectsModel") BuiltinClassesModel classesModel,
                           @Cached("classesModel.listShape") Shape listShape,
                           @Cached("classesModel.listObject") BladeClass listObject) {
    return new ListObject(listShape, listObject, new Object[0]);
  }

  @Specialization(guards = {"items.length > 0", "lower != upper"})
  protected static Object doList3(Node node, ListObject list, long lower, long upper,
                           @Cached(value = "list.items", dimensions = 1) Object[] items,
                           @Cached(value = "get(node)") BladeContext context,
                           @Cached(value = "context.objectsModel") BuiltinClassesModel classesModel,
                           @Cached @Cached.Shared("normalizeLowerIndexNode") NNormalizeIndexNode normalizeLowerIndexNode,
                           @Cached @Cached.Shared("normalizeUpperIndexNode") NNormalizeIndexNode normalizeUpperIndexNode,
                           @Cached("classesModel.listShape") Shape listShape,
                           @Cached("classesModel.listObject") BladeClass listObject) {
    final int length = items.length;
    final int effectiveLower = normalizeLowerIndexNode.executeLong(node, lower, length);
    final int effectiveUpper = normalizeUpperIndexNode.executeLong(node, upper, length);

    if (effectiveUpper < effectiveLower) {
      return new ListObject(listShape, listObject, new Object[0]);
    }

    int effectiveLength = effectiveUpper - effectiveLower;
    Object[] objects = new Object[effectiveLength];
    System.arraycopy(items, effectiveLower, objects, 0, effectiveLength);

    return new ListObject(listShape, listObject, objects);
  }

  @Fallback
  protected static Object doFallback(Node node, Object object, Object lower, Object upper) {
    throw BladeRuntimeError.error(
      node,
      "Object of type ",
      BladeUtil.getObjectType(object),
      " does not support slice operations"
    );
  }
}
