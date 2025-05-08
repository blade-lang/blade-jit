package org.nimbus.language.nodes;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem({boolean.class, int.class, double.class})
public class NimTypes {

  /*@TypeCheck(double.class)
  public static boolean isDouble(Object value) {
    return value instanceof Double || value instanceof Long || value instanceof Integer;
  }

  @TypeCast(double.class)
  public static double asDouble(Object value) {
    if(value instanceof Long longVal) {
      return longVal.doubleValue();
    } else if(value instanceof Integer intVal) {
      return intVal.doubleValue();
    }

    return (double) value;
  }*/
  // THE ABOVE COMMENTED CODE DOES THE SAME AS THE NEXT @ImplicitCast METHOD
  @ImplicitCast
  public static double castLongToDouble(long value) {
    return value;
  }

  @ImplicitCast
  public static double castIntToDouble(int value) {
    return value;
  }
}
