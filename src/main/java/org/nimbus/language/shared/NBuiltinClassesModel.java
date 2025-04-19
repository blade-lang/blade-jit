package org.nimbus.language.shared;

import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.runtime.NClassObject;

public class NBuiltinClassesModel {
  public final Shape rootShape;
  public final Shape listShape;
  public final NClassObject functionObject;
  public final NClassObject listObject;
  public final NClassObject stringObject;

  public NBuiltinClassesModel(
    Shape rootShape, Shape listShape, NClassObject functionObject,
    NClassObject listObject, NClassObject stringObject
  ) {
    this.rootShape = rootShape;
    this.listShape = listShape;
    this.functionObject = functionObject;
    this.listObject = listObject;
    this.stringObject = stringObject;
  }
}
