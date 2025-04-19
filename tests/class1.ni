class A {
  A() {
    self.value = 1
  }

  a() {
      return self.value
  }
}

echo new A().a()