package org.blade.language.nodes;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem({boolean.class, long.class, double.class})
public class BladeTypes {

  /*@TypeCheck(double.class)
  public static boolean isDouble(Object value) {
    return value instanceof Double || value instanceof Long;
  }

  @TypeCast(double.class)
  public static double asDouble(Object value) {
    if(value instanceof Long) {
      return ((Long) value).doubleValue();
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
