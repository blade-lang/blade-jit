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

catch {
  main()
} as e {
  echo e.stacktrace
}