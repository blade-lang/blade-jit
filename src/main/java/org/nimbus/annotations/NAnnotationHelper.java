package org.nimbus.annotations;

public class NAnnotationHelper {
  public static String getObjectName(Class<?> c) {
    if(c.isAnnotationPresent(ObjectName.class)) {
      ObjectName name = c.getAnnotation(ObjectName.class);
      return name.value();
    }

    return c.getName();
  }
}
