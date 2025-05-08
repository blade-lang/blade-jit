package org.nimbus.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegulatedMap<Key, Regulator, Value> {

  public record RegulatedMapEntry<A, B, C>(A key, B regulator, C value) {}

  private final List<RegulatedMapEntry<Key, Regulator, Value>> data = new ArrayList<>();

  public void add(Key key, Regulator regulator, Value value) {
    data.add(new RegulatedMapEntry<>(key, regulator, value));
  }

  public List<RegulatedMapEntry<Key, Regulator, Value>> toList() {
    return new ArrayList<>(data);
  }

  public void forEach(Consumer<? super RegulatedMapEntry<Key, Regulator, Value>> callback) {
    if(callback != null) {
      data.forEach(callback);
    }
  }
}
