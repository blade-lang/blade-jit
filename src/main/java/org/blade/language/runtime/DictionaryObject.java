package org.blade.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

import java.util.ArrayList;
import java.util.List;

@ExportLibrary(InteropLibrary.class)
public class DictionaryObject extends BladeObject {
  private static final DynamicObjectLibrary UNCACHED_OBJ = DynamicObjectLibrary.getUncached();

  public DictionaryObject(Shape shape, BladeClass classObject, Object[] keys, Object[] values) {
    super(shape, classObject);
    setEntries(keys, values);
  }

  @ExportMessage
  Object getMetaObject() throws UnsupportedMessageException {
    return BladeType.DICTIONARY;
  }

  @ExplodeLoop
  @Override
  public String toString() {
    Object[] keys = getMembers(false, UNCACHED_OBJ).getNames();

    List<String> builder = new ArrayList<>();
    for (Object key : keys) {
      try {
        builder.add(BString.concatString(
          key.toString(),
          ": ",
          UNCACHED_OBJ.getOrDefault(this, key, BladeNil.SINGLETON)
        ));
      } catch (Exception ignored) {
      }
    }

    String result = "{" + BString.join(", ", builder) + "}";
    builder.clear();

    return result;
  }

  private void setEntries(Object[] keys, Object[] values) {
    int keysLength = keys.length;
    assert keysLength == values.length;

    for(int i = 0; i < keysLength; i++) {
      UNCACHED_OBJ.put(this, BString.toString(keys[i]), values[i]);
    }
  }
}
