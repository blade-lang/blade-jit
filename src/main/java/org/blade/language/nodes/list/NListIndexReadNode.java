package org.blade.language.nodes.list;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.BladeTypesGen;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.NSharedPropertyReaderNode;
import org.blade.language.nodes.expressions.NParentExprNode;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BladeRuntimeError;
import org.blade.language.runtime.ListObject;

@SuppressWarnings("truffle-inlining")
@NodeChild("listExpr")
@NodeChild("indexExpr")
public abstract class NListIndexReadNode extends NNode {
  protected abstract NNode getListExpr();

  protected abstract NNode getIndexExpr();

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private InnerNode innerNode = NListIndexReadNodeGen.InnerNodeGen.create();

  @Specialization
  protected Object doIndexOrProperty(Object target, Object indexOrProperty) {
    return innerNode.executeRead(target, indexOrProperty);
  }

  @Override
  public Object evaluateReceiver(VirtualFrame frame) {
    return getListExpr().execute(frame);
  }

  @Override
  public Object evaluateFunction(VirtualFrame frame, Object receiver) {
    Object property = getIndexExpr().execute(frame);

    NNode expr = getListExpr();
    Object target = expr instanceof NParentExprNode parentNode
      ? parentNode.getParentClass()
      : receiver;

    return doIndexOrProperty(target, property);
  }


  @ImportStatic(BString.class)
  static abstract class InnerNode extends Node {
    abstract Object executeRead(Object list, Object index);

    @Specialization(guards = "listLibrary.isArrayElementReadable(list, index)", limit = "3")
    protected Object doListLong(Object list, long index,
                                @CachedLibrary("list") InteropLibrary listLibrary) {
      try {
        return listLibrary.readArrayElement(list, index);
      } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
        throw BladeRuntimeError.error(this, e.getMessage());
      }
    }

    @Specialization(guards = "equals(property, cachedProperty, equalNode)", limit = "3")
    protected Object doListStringCached(
      Object list, TruffleString property,
      @Cached("property") TruffleString cachedProperty,
      @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
      @Cached("toJavaStringNode.execute(cachedProperty)") String cachedJavaString,
      @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode,
      @Cached TruffleString.EqualNode equalNode
    ) {
      return propertyReaderNode.executeRead(list, cachedJavaString);
    }

    @Specialization(replaces = "doListStringCached")
    protected Object doListString(
      Object list, TruffleString property,
      @Cached @Cached.Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode,
      @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode
    ) {
      return propertyReaderNode.executeRead(list, toJavaStringNode.execute(property));
    }

    @Specialization(guards = "listLibrary.isNull(list)", limit = "3")
    protected Object doNil(Object list, long index,
                           @CachedLibrary("list") InteropLibrary listLibrary) {
      throw BladeRuntimeError.error(this, "Cannot read properties of nil (reading '", index, "')");
    }

    @Specialization(guards = "interopLibrary.hasMembers(list)", limit = "3")
    protected Object doNonString(
      Object list, Object property,
      @CachedLibrary("list") InteropLibrary interopLibrary,
      @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode
    ) {
      return propertyReaderNode.executeRead(list, BString.toString(property));
    }

    @Fallback
    protected Object doUnsupported(
      Object list, Object index,
      @Cached @Cached.Shared("propertyReaderNode") NSharedPropertyReaderNode propertyReaderNode
    ) {
      if (list instanceof ListObject && BladeTypesGen.isImplicitDouble(index)) {
        throw BladeRuntimeError.error(this, "List index ", index, " out of range");
      }
      return propertyReaderNode.executeRead(list, index);
    }
  }
}
