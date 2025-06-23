package org.blade.language.shared;

import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.runtime.BString;
import org.blade.language.runtime.BladeClass;

import java.util.HashMap;
import java.util.Map;

public class ErrorsModel {
  public final BladeClass error;
  public final BladeClass typeError;
  public final BladeClass argumentError;
  public final BladeClass valueError;
  public final BladeClass assertError;

  public final Map<TruffleString, BladeClass> ALL = new HashMap<>();

  public ErrorsModel(BladeClass error, BladeClass typeError, BladeClass argumentError, BladeClass valueError, BladeClass assertError) {
    this.error = error;
    this.typeError = typeError;
    this.argumentError = argumentError;
    this.valueError = valueError;
    this.assertError = assertError;

    ALL.put(BString.fromJavaString("Error"), error);
    ALL.put(BString.fromJavaString("TypeError"), typeError);
    ALL.put(BString.fromJavaString("ArgumentError"), argumentError);
    ALL.put(BString.fromJavaString("ValueError"), valueError);
    ALL.put(BString.fromJavaString("AssertError"), assertError);
  }
}
