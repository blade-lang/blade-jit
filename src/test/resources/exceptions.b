def main() {
  f1()
}
def f1() {
  var x = f2()
  return x
}
def f2() {
  return f3()
}
def f3() {
  raise new Error('Exception in f3()')
}

try {
  main()
} catch e {
  echo e.stacktrace
}

class Countdown {
  @new(start) {
    self.count = start
  }

  decrement() {
    if self.count <= 0 {
      raise new ValueError('countdown has completed')
    }
    self.count = self.count - 1
  }
}

def countdown(n) {
  const countdown = new Countdown(n)
  var ret = 0

  iter ;; {
    try {
      countdown.decrement()
      ret = ret + 1
    } catch e {
      echo e.stacktrace
      break
    }
  }

  return ret
}

countdown(1000000000)

class NameError < ValueError {}

raise new NameError('Some errors')