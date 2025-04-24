package org.nimbus.language.shared;

import org.nimbus.language.runtime.NimClass;

import java.util.HashMap;
import java.util.Map;

public class NErrorsModel {
  public final NimClass error;
  public final NimClass typeError;
  public final NimClass argumentError;
  public final NimClass valueError;

  public final Map<String, NimClass> ALL = new HashMap<>();

  public NErrorsModel(NimClass error, NimClass typeError, NimClass argumentError, NimClass valueError) {
    this.error = error;
    this.typeError = typeError;
    this.argumentError = argumentError;
    this.valueError = valueError;

    ALL.put("Error", error);
    ALL.put("TypeError", typeError);
    ALL.put("ArgumentError", argumentError);
    ALL.put("ValueError", valueError);
  }
}
