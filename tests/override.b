class A {
  @abs() {
    return 300
  }

  to_string() {
    return 'A class called A'
  }
}

echo abs(-10)
echo abs(new A())
echo new A().to_string()
