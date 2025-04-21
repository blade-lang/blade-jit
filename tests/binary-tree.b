const N = 21

def make_tree(depth) {
  if depth <= 0 return [nil, nil]
  depth -= 1
  return [make_tree(depth), make_tree(depth)]
}

def check_tree(node) {
  var left = node[0], right = node[1]
  if !left return 1
  return 1 + check_tree(left) + check_tree(right)
}

def max(a, b) {
  return a > b ? a : b
}

var start = time()

var min_depth = 4
var max_depth = max(min_depth + 2, N)
var stretch_depth = max_depth + 1

var stretch_tree = make_tree(stretch_depth)

echo 'stretch tree of depth ${stretch_depth}\t check: ${check_tree(stretch_tree)}'
stretch_tree = nil

var long_lived_tree = make_tree(max_depth)

iter var depth = min_depth; depth < stretch_depth; depth += 2 {
  var iterations = 2 ** (max_depth - depth + min_depth)

  var check = 0
  iter var i = 1; i < iterations + 1; i++ {
    check += check_tree(make_tree(depth))
  }

  echo '${iterations}\t trees of depth ${depth}\t check: ${check}'
}

echo 'long lived tree of depth ${max_depth}\t check: ${check_tree(long_lived_tree)}'
echo 'Total time taken: ${time() - start}'
