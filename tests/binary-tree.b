def run(N) {
   const maxDepth = N

   const stretchDepth = maxDepth + 1
   const check = itemCheck(bottomUpTree(stretchDepth))
   echo 'stretch tree of depth ${stretchDepth}\t check: ${check}'

   const longLivedTree = bottomUpTree(maxDepth)

   iter var depth = 4; depth <= maxDepth; depth += 2 {
       const iterations = 1 << maxDepth - depth + 4
       work(iterations, depth)
   }

   echo 'long lived tree of depth ${maxDepth}\t check: ${itemCheck(longLivedTree)}'
}

def work(iterations, depth) {
   var check = 0
   iter var i = 0; i < iterations; i++ {
       check += itemCheck(bottomUpTree(depth))
   }
   echo '${iterations}\t trees of depth ${depth}\t check: ${check}'
}

class TreeNode {
   @new(left, right) {
      self.left = left
      self.right = right
   }
}

def itemCheck(node) {
   if node.left == nil {
       return 1
   }
   return 1 + itemCheck(node.left) + itemCheck(node.right)
}

def bottomUpTree(depth) {
   return depth > 0 ?
       new TreeNode(bottomUpTree(depth - 1), bottomUpTree(depth - 1)) :
       new TreeNode(nil, nil)
}

var start = microtime()
run(21)
echo 'Total time taken: ${(microtime() - start) / 1.0e+6}s'
