package org.zuri.language.shared;

import com.oracle.truffle.api.object.Shape;
import org.zuri.language.runtime.BObject;
import org.zuri.language.runtime.ZuriClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BuiltinClassesModel {
  public final Shape rootShape;
  public final Shape listShape;
  public final Shape dictionaryShape;
  public final BObject objectObject;
  public final ZuriClass functionObject;
  public final ZuriClass listObject;
  public final ZuriClass dictionaryObject;
  public final ZuriClass stringObject;
  public final ZuriClass numberObject;
  public final ZuriClass bigIntObject;
  public final ZuriClass booleanObject;
  public final ZuriClass rangeObject;

  public final ErrorsModel errorsModel;
  public final Map<String, ZuriClass> builtinClasses;

  public BuiltinClassesModel(
    Shape rootShape, Shape listShape, Shape dictionaryShape, BObject objectObject,
    ZuriClass functionObject, ErrorsModel errorsModel
  ) {
    this.rootShape = rootShape;
    this.listShape = listShape;
    this.dictionaryShape = dictionaryShape;
    this.objectObject = objectObject;
    this.functionObject = functionObject;
    this.listObject = new ZuriClass(rootShape, "List", objectObject, true);
    this.dictionaryObject = new ZuriClass(rootShape, "Dictionary", objectObject, true);
    this.stringObject = new ZuriClass(rootShape, "String", objectObject, true);
    this.numberObject = new ZuriClass(rootShape, "Number", objectObject, true);
    this.booleanObject = new ZuriClass(rootShape, "Bool", objectObject, true);
    this.bigIntObject = new ZuriClass(rootShape, "BigInt", objectObject, true);
    this.rangeObject = new ZuriClass(rootShape, "Range", objectObject, true);
    this.errorsModel = errorsModel;

    Map<String, ZuriClass> allBuiltInClasses = new HashMap<>();
    allBuiltInClasses.put("Object", objectObject);
    allBuiltInClasses.putAll(errorsModel.ALL);
    builtinClasses = Collections.unmodifiableMap(allBuiltInClasses);
  }
}
