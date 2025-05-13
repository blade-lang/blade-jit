package org.blade.language.shared;

import com.oracle.truffle.api.object.Shape;
import org.blade.language.runtime.BObject;
import org.blade.language.runtime.BladeClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BuiltinClassesModel {
  public final Shape rootShape;
  public final Shape listShape;
  public final Shape dictionaryShape;
  public final BObject objectObject;
  public final BladeClass functionObject;
  public final BladeClass listObject;
  public final BladeClass dictionaryObject;
  public final BladeClass stringObject;
  public final BladeClass numberObject;
  public final BladeClass booleanObject;
  public final BladeClass rangeObject;

  public final ErrorsModel errorsModel;
  public final Map<String, BladeClass> builtinClasses;

  public BuiltinClassesModel(
    Shape rootShape, Shape listShape, Shape dictionaryShape, BObject objectObject,
    BladeClass functionObject, BladeClass listObject, BladeClass dictionaryObject, BladeClass stringObject,
    BladeClass numberObject, BladeClass booleanObject, BladeClass rangeObject,
    ErrorsModel errorsModel
  ) {
    this.rootShape = rootShape;
    this.listShape = listShape;
    this.dictionaryShape = dictionaryShape;
    this.objectObject = objectObject;
    this.functionObject = functionObject;
    this.listObject = listObject;
    this.dictionaryObject = dictionaryObject;
    this.stringObject = stringObject;
    this.numberObject = numberObject;
    this.booleanObject = booleanObject;
    this.rangeObject = rangeObject;
    this.errorsModel = errorsModel;

    Map<String, BladeClass> allBuiltInClasses = new HashMap<>();
    allBuiltInClasses.put("Object", objectObject);
    allBuiltInClasses.putAll(errorsModel.ALL);
    builtinClasses = Collections.unmodifiableMap(allBuiltInClasses);
  }
}
