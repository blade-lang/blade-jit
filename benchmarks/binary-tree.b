var N = 21

class Tree {
  @new(left, right) {
    self.left = right
    self.right = right
  }
}

def max(a, b) {
  return a > b ? a : b
}

def make_tree(depth) {
  if depth <= 0 return [nil, nil]
  depth -= 1
  return [make_tree(depth), make_tree(depth)]
}

def check_tree(node) {
  if node[0] == nil return 1
  return 1 + check_tree(node[0]) + check_tree(node[1])
}

var start = microtime()

var min_depth = 4
var max_depth = max(min_depth + 2, N)
var stretch_depth = max_depth + 1

echo 'stretch tree of depth ${stretch_depth}\t check: ${check_tree(make_tree(stretch_depth))}'

var long_lived_tree = make_tree(max_depth)

var iterations = 2 ** max_depth

iter var depth = min_depth; depth < stretch_depth; depth += 2 {
  var check = 0
  iter var i = 1; i < (iterations + 1); i++ {
    check += check_tree(make_tree(depth))
  }

  echo '${iterations}\t trees of depth ${depth}\t check: ${check}'
  iterations //= 4
}

echo 'long lived tree of depth ${max_depth}\t check: ${check_tree(long_lived_tree)}'
echo 'Total time taken: ${(microtime() - start)/1.0e+6}'
