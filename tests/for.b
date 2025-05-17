for i in [1,2,3,4,5] {
  echo i
  if i == 4 break
}

var details = {name: 'Richard', age: 27, address: 'Nigeria'}

for x, y in details {
  echo '${x} = ${y}'

  for i, j in [6,7,8,9,10] {
    echo '${i} = ${j}'

    for i in [11,12,13,14,15] {
      echo i
    }
  }
}

for n in 'name' {
  echo n
}

class Iterable {
  var items = ['Richard', 'Alex', 'Justina']

  @value(x) {
    return self.items[x]
  }

  @key(x) {
    if x == nil return 0

    if x < self.items.length - 1
      return x + 1
    return nil
  }
}

for it in new Iterable() {
  echo it
}
