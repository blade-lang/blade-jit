package org.nimbus.language.shared;

import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.runtime.NObject;
import org.nimbus.language.runtime.NimClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NBuiltinClassesModel {
  public final Shape rootShape;
  public final Shape listShape;
  public final NObject objectObject;
  public final NimClass functionObject;
  public final NimClass listObject;
  public final NimClass stringObject;

  public final NErrorsModel errorsModel;
  public final Map<String, NimClass> builtinClasses;

  public NBuiltinClassesModel(
    Shape rootShape, Shape listShape, NObject objectObject,
    NimClass functionObject, NimClass listObject, NimClass stringObject,
    NErrorsModel errorsModel
  ) {
    this.rootShape = rootShape;
    this.listShape = listShape;
    this.objectObject = objectObject;
    this.functionObject = functionObject;
    this.listObject = listObject;
    this.stringObject = stringObject;
    this.errorsModel = errorsModel;

    Map<String, NimClass> allBuiltInClasses = new HashMap<>();
    allBuiltInClasses.put("Object", objectObject);
    allBuiltInClasses.putAll(errorsModel.ALL);
    builtinClasses = Collections.unmodifiableMap(allBuiltInClasses);
  }
}
