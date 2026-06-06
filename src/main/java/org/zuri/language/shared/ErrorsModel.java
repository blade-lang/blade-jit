package org.zuri.language.shared;

import org.zuri.language.runtime.ZuriClass;

import java.util.HashMap;
import java.util.Map;

public class ErrorsModel {
  public final ZuriClass error;
  public final ZuriClass typeError;
  public final ZuriClass argumentError;
  public final ZuriClass valueError;
  public final ZuriClass assertError;

  public final Map<String, ZuriClass> ALL = new HashMap<>();

  public ErrorsModel(ZuriClass error, ZuriClass typeError, ZuriClass argumentError, ZuriClass valueError, ZuriClass assertError) {
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
