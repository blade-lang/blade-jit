package org.blade.language.shared;

import org.blade.language.runtime.BladeClass;

import java.util.HashMap;
import java.util.Map;

public class ErrorsModel {
  public final BladeClass error;
  public final BladeClass typeError;
  public final BladeClass argumentError;
  public final BladeClass valueError;
  public final BladeClass assertError;

  public final Map<String, BladeClass> ALL = new HashMap<>();

  public ErrorsModel(BladeClass error, BladeClass typeError, BladeClass argumentError, BladeClass valueError, BladeClass assertError) {
    this.error = error;
    this.typeError = typeError;
    this.argumentError = argumentError;
    this.valueError = valueError;
    this.assertError = assertError;

    ALL.put("Error", error);
    ALL.put("TypeError", typeError);
    ALL.put("ArgumentError", argumentError);
    ALL.put("ValueError", valueError);
    ALL.put("AssertError", assertError);
  }
}
