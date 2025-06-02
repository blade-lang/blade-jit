/**
 * Base On: https://benchmarksgame-team.pages.debian.net/benchmarksgame/program/mandelbrot-graalvmaot-1.html
 *
 * The Computer Language Benchmarks Game
 * https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
 * contributed by Stefan Krause
 * slightly modified by Chad Whipkey
 */
const BUFFER_SIZE = 8_192

class Mandelbrot {
  var size
  var buf = [0] * BUFFER_SIZE
  var bufLen = 0
  var fac
  var shift

  @new(size) {
    self.size = size
    self.fac = 2.0 / size
    self.shift = size % 8 == 0 ? 0 : (8 - size % 8)
  }

  compute()
  {
    echo "P4\n${self.size} ${self.size}\n"
    iter var y = 0; y<self.size; y++ {
      self.computeRow(y)
    }
    print(self.buf)
  }

  computeRow(y)
  {
    var bits = 0

    var Ci = (y*self.fac - 1.0)
    var bufLocal = self.buf
    iter var x = 0; x<self.size;x++ {
      var Zr = 0.0
      var Zi = 0.0
      var Cr = (x*self.fac - 1.5)
      var i = 50
      var ZrN = 0
      var ZiN = 0
      do {
        Zi = 2.0 * Zr * Zi + Ci
        Zr = ZrN - ZiN + Cr
        ZiN = Zi * Zi
        ZrN = Zr * Zr
      } while (!(ZiN + ZrN > 4.0) and i-- > 0)

      bits = bits << 1
      if (i == 0) bits++

      if (x%8 == 7) {
        bufLocal[self.bufLen++ - 1] = bits
        if (self.bufLen == BUFFER_SIZE) {
          print(bufLocal)
          self.bufLen = 0
        }
        bits = 0
      }
    }
    if (self.shift!=0) {
      bits = bits << self.shift
      bufLocal[bufLen++ - 1] = bits
      if (self.bufLen == BUFFER_SIZE) {
        print(bufLocal)
        self.bufLen = 0
      }
    }
  }
}

var start = microtime()
new Mandelbrot(16_000).compute()

echo '\nTotal time taken = ${(microtime() - start) / 1_000_000}s'
