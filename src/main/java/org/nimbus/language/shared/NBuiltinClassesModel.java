package org.nimbus.language.shared;

import com.oracle.truffle.api.object.Shape;
import org.nimbus.language.runtime.NObject;
import org.nimbus.language.runtime.NimClass;

public class NBuiltinClassesModel {
  public final Shape rootShape;
  public final Shape listShape;
  public final NObject objectObject;
  public final NimClass functionObject;
  public final NimClass listObject;
  public final NimClass stringObject;

  public NBuiltinClassesModel(
    Shape rootShape, Shape listShape, NObject objectObject, NimClass functionObject,
    NimClass listObject, NimClass stringObject
  ) {
    this.rootShape = rootShape;
    this.listShape = listShape;
    this.objectObject = objectObject;
    this.functionObject = functionObject;
    this.listObject = listObject;
    this.stringObject = stringObject;
  }
}
