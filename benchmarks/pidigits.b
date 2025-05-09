/* The Computer Language Benchmarks Game
   https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

   transliterated from Alexander Fyodorov's program by Isaac Gouy
*/

def pad(i, last) {
  var res = i.to_string(), count
  count = 10 - res.length
  while count > 0 {
    if last {
      res += ' '
    } else {
      res = '0' + res
    }
    count--
  }
  return res
}

def calculatePi(arg) {
  var i = 0, ns = 0

  var k = 0
  var k1 = 1
  var a = 0
  var d = 1
  var m = 0
  var n = 1
  var t = 0
  var u = 1

  while true {
    k += 1
    k1 += 2
    t = n << 1
    n *= k
    a += t
    a *= k1
    d *= k1

    if a > n {
      m = n * 3 + a
      t = m / d
      u = m % d + n

      if d > u {
        ns = ns * 10 + to_number(t)
        i += 1

        var last = i >= arg
        if i % 10 == 0 or last {
                
          echo pad(ns, last) + '\t:' + i
          ns = 0
        }

        if last break

        a = (a - d * t) * 10
        n = n * 10
      }
    }
  }
}


var start = microtime()
calculatePi(10000)
echo '\nTotal time taken = ${(microtime() - start) / 1000000}'