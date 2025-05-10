def generate_fibonacci(n) {
  var fib = [1] * n
#  for i in 2..n {
#    fib[i] = fib[i - 2] + fib[i - 1]
#  }
  iter var i = 2; i < n; i++ {
    fib[i] = fib[i - 2] + fib[i - 1]
  }
  return fib
}

var fib = generate_fibonacci(35)
echo fib