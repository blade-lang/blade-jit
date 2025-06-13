package org.blade;

import org.graalvm.polyglot.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NGetPropertyTest {
  private Context context;

  @Before
  public void setup() {
    context = Context.create();
  }

  @After
  public void tearDown() {
    context.close();
  }

  @Test
  public void bubble_sort_changes_array_to_sorted() {
    var result = context.eval(
      "blade", """
        var array = [44, 33, 22, 11]
        def bubbleSort(array) {
            iter var i = 0; i < array.length - 1; i = i + 1 {
                iter var j = 0; j < array.length - 1 - i; j = j + 1 {
                    if array[j] > array[j + 1] {
                        var tmp = array[j]
                        array[j] = array[j + 1]
                        array[j + 1] = tmp
                    }
                }
            }
        }
        bubbleSort(array)
        array"""
    );

    assertEquals(11, result.getArrayElement(0).asInt());
    assertEquals(22, result.getArrayElement(1).asInt());
    assertEquals(33, result.getArrayElement(2).asInt());
    assertEquals(44, result.getArrayElement(3).asInt());
  }
}
