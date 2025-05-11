package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.blade.annotations.ObjectName;
import org.blade.language.BladeLanguage;

import java.math.BigDecimal;
import java.math.BigInteger;

@ExportLibrary(InteropLibrary.class)
@ObjectName("BigInt")
public final class BigIntObject implements TruffleObject, Comparable<BigIntObject> {

  private final BigInteger value;

  public BigIntObject(BigInteger value) {
    this.value = value;
  }

  public BigIntObject(long value) {
    this.value = BigInteger.valueOf(value);
  }

  public BigInteger get() {
    return value;
  }

  @CompilerDirectives.TruffleBoundary
  @Override
  public int compareTo(BigIntObject o) {
    return value.compareTo(o.get());
  }

  @Override
  @CompilerDirectives.TruffleBoundary
  public String toString() {
    return value.toString();
  }

  @Override
  @CompilerDirectives.TruffleBoundary
  public boolean equals(Object obj) {
    if (obj instanceof BigIntObject) {
      return value.equals(((BigIntObject) obj).get());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @SuppressWarnings("static-method")
  @ExportMessage
  boolean isNumber() {
    return true;
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  boolean fitsInByte() {
    return value.bitLength() < 8;
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  boolean fitsInShort() {
    return value.bitLength() < 16;
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  boolean fitsInFloat() {
    if (value.bitLength() <= 24) { // 24 = size of float mantissa + 1
      return true;
    } else {
      float floatValue = value.floatValue();
      if (!Float.isFinite(floatValue)) {
        return false;
      }
      try {
        return new BigDecimal(floatValue).toBigIntegerExact().equals(value);
      } catch (ArithmeticException e) {
        throw CompilerDirectives.shouldNotReachHere(e);
      }
    }
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  boolean fitsInLong() {
    return value.bitLength() < 64;
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  boolean fitsInInt() {
    return value.bitLength() < 32;
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  boolean fitsInDouble() {
    if (value.bitLength() <= 53) { // 53 = size of double mantissa + 1
      return true;
    } else {
      double doubleValue = value.doubleValue();
      if (!Double.isFinite(doubleValue)) {
        return false;
      }
      try {
        return new BigDecimal(doubleValue).toBigIntegerExact().equals(value);
      } catch (ArithmeticException e) {
        throw CompilerDirectives.shouldNotReachHere(e);
      }
    }
  }

  @ExportMessage
  public boolean fitsInBigInteger() {
    return true;
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  double asDouble() throws UnsupportedMessageException {
    if (fitsInDouble()) {
      return value.doubleValue();
    } else {
      throw UnsupportedMessageException.create();
    }
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  long asLong() throws UnsupportedMessageException {
    if (fitsInLong()) {
      return value.longValue();
    } else {
      throw UnsupportedMessageException.create();
    }
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  byte asByte() throws UnsupportedMessageException {
    if (fitsInByte()) {
      return value.byteValue();
    } else {
      throw UnsupportedMessageException.create();
    }
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  int asInt() throws UnsupportedMessageException {
    if (fitsInInt()) {
      return value.intValue();
    } else {
      throw UnsupportedMessageException.create();
    }
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  float asFloat() throws UnsupportedMessageException {
    if (fitsInFloat()) {
      return value.floatValue();
    } else {
      throw UnsupportedMessageException.create();
    }
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  short asShort() throws UnsupportedMessageException {
    if (fitsInShort()) {
      return value.shortValue();
    } else {
      throw UnsupportedMessageException.create();
    }
  }

  @ExportMessage
  BigInteger asBigInteger() {
    return value;
  }

  @ExportMessage
  boolean hasLanguage() {
    return true;
  }

  @ExportMessage
  Class<? extends TruffleLanguage<?>> getLanguage() {
    return BladeLanguage.class;
  }

  @ExportMessage
  boolean hasMetaObject() {
    return true;
  }

  @ExportMessage
  Object getMetaObject() {
    return BladeType.NUMBER;
  }

  @ExportMessage
  @CompilerDirectives.TruffleBoundary
  Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
    return value.toString();
  }
}
