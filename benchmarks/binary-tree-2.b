# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
#
# contributed by Jesse Millikan
# Modified by Wesley Moxam
# Modified by Scott Leggett
# *reset*

class Node {
  @new(left, right) {
    self.left = left
    self.right = right
  }

  check() {
    if !self.left {
      return 1
    }
    return 1 + self.left.check() + self.right.check()
  }
}

def make_tree(depth) {
    if  depth > 0 {
        return new Node(make_tree(depth - 1), make_tree(depth - 1))
    } else  {
        return new Node()
    }
}

var start = microtime()

const min_depth = 4
const max_depth = 21

const stretch_depth = max_depth + 1

echo "stretch tree of depth ${stretch_depth}\t check: ${make_tree(stretch_depth).check()}"

var long_lived_tree = make_tree(max_depth)
echo max_depth
echo long_lived_tree.left

iter var depth = min_depth; depth <= max_depth; depth += 2 {
  var iterations = 1 << (max_depth - depth + min_depth)
  var check = 0

  iter var i = 1; i <= iterations; i++ {
    check += make_tree(depth).check()
  }

  echo "${iterations}\t trees of depth ${depth}\t check: ${check}"
}

echo "long lived tree of depth ${max_depth}\t check: ${long_lived_tree.check()}"
echo "Took ${(microtime() - start) / 1000000}"