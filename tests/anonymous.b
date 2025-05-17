@{
  echo 'Self called'
}()

var fn = @{
  return 10
}

var fn2 = @{
  return 20
}

var fn3 = @(val) {
  return val ** 5
}

var fn4 = @(val, ...arg) {
  return '${val} is not in list ${arg}'
}

echo fn()
echo fn2()
echo fn3(14)
echo fn4(40, 50, 60)

class Demo {
  var multiply = @(val) {
    self.value = 1_000 * val
    return val * 30
  }

  get_val() {
    return self.value
  }
}

const demo = new Demo()
echo demo.multiply(15)
echo demo.get_val()
echo Demo.multiply(46)
