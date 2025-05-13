package org.blade.language.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.blade.language.BladeLanguage;

import java.util.Objects;

@ExportLibrary(InteropLibrary.class)
public abstract class DebugObject implements TruffleObject {
  protected abstract RefObject[] getRefs();

  static int CACHE_LIMIT = 4;

  protected final Frame frame;

  public DebugObject(Frame frame) {
    this.frame = frame;
  }

  @ExportMessage
  boolean isScope() {
    return true;
  }

  @ExportMessage
  boolean hasLanguage() {
    return true;
  }

  @ExportMessage
  Class<? extends TruffleLanguage<?>> getLanguage() {
    return BladeLanguage.class;
  }

  /* We need this method to satisfy the Truffle DSL validation. */
  @ExportMessage
  Object toDisplayString(boolean allowSideEffects) {
    throw new UnsupportedOperationException();
  }

  @ExportMessage
  boolean isMemberInsertable(String member) {
    return false;
  }

  @ExportMessage
  boolean hasMembers() {
    return true;
  }

  @ExportMessage
  Object getMembers(boolean includeInternal) {
    RefObject[] references = getRefs();
    return new RefObjectList(references);
  }

  @ExportMessage(name = "isMemberReadable")
  static final class MemberReadable {
    @Specialization(limit = "CACHE_LIMIT", guards = "cachedMember.equals(member)")
    static boolean doCached(DebugObject receiver, String member,
                            @Cached("member") String cachedMember,
                            @Cached("doUncached(receiver, member)") boolean cachedResult) {
      return cachedResult;
    }

    @Specialization(replaces = "doCached")
    static boolean doUncached(DebugObject receiver, String member) {
      return receiver.referenceCalled(member);
    }
  }

  @ExportMessage(name = "readMember")
  static final class ReadMember {
    @Specialization(limit = "CACHE_LIMIT", guards = "cachedMember.equals(member)")
    static Object doCached(DebugObject receiver, String member,
                           @Cached("member") String cachedMember,
                           @Cached("receiver.findReference(member)") RefObject refObject
    ) throws UnknownIdentifierException {
      return readMember(receiver, cachedMember, refObject);
    }

    @Specialization(replaces = "doCached")
//    @CompilerDirectives.TruffleBoundary
    static Object doUncached(DebugObject receiver, String member) throws UnknownIdentifierException {
      RefObject refObject = receiver.findReference(member);
      return readMember(receiver, member, refObject);
    }

    private static Object readMember(DebugObject receiver, String member, RefObject refObject) throws UnknownIdentifierException {
      if (refObject == null) {
        throw UnknownIdentifierException.create(member);
      }
      return refObject.read(receiver.frame);
    }
  }

  @ExportMessage(name = "isMemberModifiable")
  static final class MemberModifiable {
    @Specialization(limit = "CACHE_LIMIT", guards = "cachedMember.equals(member)")
    static boolean doCached(DebugObject receiver, String member,
                            @Cached("member") String cachedMember,
                            @Cached("doUncached(receiver, member)") boolean cachedResult) {
      return cachedResult;
    }

    @Specialization(replaces = "doCached")
    static boolean doUncached(DebugObject receiver, String member) {
      return receiver.referenceCalled(member);
    }
  }

  @ExportMessage(name = "writeMember")
  static final class WriteMember {
    @Specialization(limit = "CACHE_LIMIT", guards = "cachedMember.equals(member)")
    static void doCached(DebugObject receiver, String member, Object value,
                         @Cached("member") String cachedMember,
                         @Cached("receiver.findReference(member)") RefObject refObject) throws UnknownIdentifierException {
      writeMember(receiver, member, refObject, value);
    }

    @Specialization(replaces = "doCached")
    static void doUncached(DebugObject receiver, String member, Object value) throws UnknownIdentifierException {
      RefObject refObject = receiver.findReference(member);
      writeMember(receiver, member, refObject, value);
    }

    private static void writeMember(DebugObject receiver, String member, RefObject refObject, Object value) throws UnknownIdentifierException {
      if (refObject == null) {
        throw UnknownIdentifierException.create(member);
      }
      refObject.write(receiver.frame, value);
    }
  }

  private boolean referenceCalled(String member) {
    return this.findReference(member) != null;
  }

  @ExplodeLoop
  RefObject findReference(String member) {
    RefObject[] refObjects = getRefs();
    for (var refObject : refObjects) {
      if (objectEquals(refObject.name, member)) {
        return refObject;
      }
    }
    return null;
  }

  @CompilerDirectives.TruffleBoundary
  private boolean objectEquals(Object a, Object b) {
    return Objects.equals(a, b);
  }
}
