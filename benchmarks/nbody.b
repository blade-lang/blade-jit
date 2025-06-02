/* The Computer Language Benchmarks Game
 * https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
 *
 * n-body Blade program
 *
 * based on work by Mark C. Lewis
 * contributed by Richard Ore
 */

var PI = 3.141592653589793
var SOLAR_MASS = 4 * PI * PI
var DAYS_PER_YEAR = 365.24

class Body {
  @new(x, y, z, vx, vy, vz, mass) {
    self.x = x
    self.y = y
    self.z = z
    self.vx = vx
    self.vy = vy
    self.vz = vz
    self.mass = mass
  }

  offsetMomentum(px, py, pz) {
    self.vx = -px / SOLAR_MASS
    self.vy = -py / SOLAR_MASS
    self.vz = -pz / SOLAR_MASS
    return self
  }
}

var Jupiter = new Body(
  4.84143144246472090e+00,
  -1.16032004402742839e+00,
  -1.03622044471123109e-01,
  1.66007664274403694e-03 * DAYS_PER_YEAR,
  7.69901118419740425e-03 * DAYS_PER_YEAR,
  -6.90460016972063023e-05 * DAYS_PER_YEAR,
  9.54791938424326609e-04 * SOLAR_MASS
)

var Saturn = new Body(
  8.34336671824457987e+00,
  4.12479856412430479e+00,
  -4.03523417114321381e-01,
  -2.76742510726862411e-03 * DAYS_PER_YEAR,
  4.99852801234917238e-03 * DAYS_PER_YEAR,
  2.30417297573763929e-05 * DAYS_PER_YEAR,
  2.85885980666130812e-04 * SOLAR_MASS
)

var Uranus = new Body(
  1.28943695621391310e+01,
  -1.51111514016986312e+01,
  -2.23307578892655734e-01,
  2.96460137564761618e-03 * DAYS_PER_YEAR,
  2.37847173959480950e-03 * DAYS_PER_YEAR,
  -2.96589568540237556e-05 * DAYS_PER_YEAR,
  4.36624404335156298e-05 * SOLAR_MASS
)

var Neptune = new Body(
  1.53796971148509165e+01,
  -2.59193146099879641e+01,
  1.79258772950371181e-01,
  2.68067772490389322e-03 * DAYS_PER_YEAR,
  1.62824170038242295e-03 * DAYS_PER_YEAR,
  -9.51592254519715870e-05 * DAYS_PER_YEAR,
  5.15138902046611451e-05 * SOLAR_MASS
)

var Sun = new Body(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, SOLAR_MASS)

var bodies = [Sun, Jupiter, Saturn, Uranus, Neptune]

class NBodySystem {
  @new() {
    var px = 0, py = 0, pz = 0

    iter var i = 0; i < bodies.length; i++ {
      var body = bodies[i]
      var mass = body.mass

      px += body.vx * mass
      py += body.vy * mass
      pz += body.vz * mass
    }

    bodies[0].offsetMomentum(px, py, pz)
  }

  advance(dt) {
    var size = bodies.length

    iter var i = 0; i < size; i++ {
      var bodyi = bodies[i]
      var massi = bodyi.mass

      var ix = bodyi.x
      var iy = bodyi.y
      var iz = bodyi.z

      iter var j = i + 1; j < size; j++ {
        var bodyj = bodies[j]

        var dx = ix - bodyj.x
        var dy = iy - bodyj.y
        var dz = iz - bodyj.z

        var d2 = dx**2 + dy**2 + dz**2
        var distance = (d2 ** 0.5) # d2 ** 0.5 = sqrt(d2)
        var mag = dt / (d2 * distance)

        var massj = bodyj.mass

        bodyi.vx -= dx * massj * mag
        bodyi.vy -= dy * massj * mag
        bodyi.vz -= dz * massj * mag

        bodyj.vx += dx * massi * mag
        bodyj.vy += dy * massi * mag
        bodyj.vz += dz * massi * mag
      }
    }

    iter var i = 0; i < size; i++ {
      var body = bodies[i]
      body.x += dt * body.vx
      body.y += dt * body.vy
      body.z += dt * body.vz
    }
  }

  energy() {
    var e = 0
    var size = bodies.length

    iter var i = 0; i < size; i++ {
      var bodyi = bodies[i]

      e += 0.5 * bodyi.mass * (bodyi.vx**2 + bodyi.vy**2 + bodyi.vz**2)

      iter var j = i + 1; j < size; j++ {
        var bodyj = bodies[j]
        var dx = bodyi.x - bodyj.x
        var dy = bodyi.y - bodyj.y
        var dz = bodyi.z - bodyj.z

        var distance = (dx**2 + dy**2 + dz**2) ** 0.5
        e -= (bodyi.mass * bodyj.mass) / distance
      }
    }

    return e
  }
}

var n = 50_000_000

var start = microtime()

var system = new NBodySystem()
echo system.energy()
iter var i = 0; i < n; i++ {
  system.advance(0.01)
}
echo system.energy()

echo '\nTotal time taken: ${(microtime() - start)/1_000_000}s'
