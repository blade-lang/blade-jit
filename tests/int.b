class Base {
  Base() {
      self.count = 0
  }

  increment() {
      self.count = self.count + 1
  }

  getCount() {
      return self.count
  }
}

class LowerMiddle < Base {
  LowerMiddle() {
    parent()
  }
}

class UpperMiddle < LowerMiddle {
  UpperMiddle() {
      parent()
  }

  increment() {
      return parent.increment()
  }

  getCount() {
      return parent.getCount()
  }
}

class Counter < UpperMiddle {
}

def countWithSelfInIterDirect(n) {
  var counter = Counter()
  iter var i = 0; i < n; i++ {
    counter.increment()
  }
  return counter.getCount()
}

var start = microtime()
echo countWithSelfInIterDirect(1000000)
echo '${(microtime() - start) / 1000}ms taken'
