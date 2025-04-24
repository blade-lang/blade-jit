package org.nimbus;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 1, time = 1)
public class BinaryTreesBenchmark extends TruffleBenchmark {
  private static final int INPUT = 21;

  private static final String NIM_SOURCE = """
    def run(N) {
         const maxDepth = N
    
         const stretchDepth = maxDepth + 1
         const check = itemCheck(bottomUpTree(stretchDepth))
         echo 'stretch tree of depth ${stretchDepth}\\t check: ${check}'
    
         const longLivedTree = bottomUpTree(maxDepth)
    
         iter var depth = 4; depth <= maxDepth; depth += 2 {
             const iterations = 1 << maxDepth - depth + 4
             work(iterations, depth)
         }
    
         echo 'long lived tree of depth ${maxDepth}\\t check: ${itemCheck(longLivedTree)}'
     }
    
     def work(iterations, depth) {
         var check = 0
         iter var i = 0; i < iterations; i++ {
             check += itemCheck(bottomUpTree(depth))
         }
         echo '${iterations}\\t trees of depth ${depth}\\t check: ${check}'
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
    """;

  private static final String JS_SOURCE = """
    function run(N) {
         const maxDepth = N;
    
         const stretchDepth = maxDepth + 1;
         const check = itemCheck(bottomUpTree(stretchDepth));
         console.log(`stretch tree of depth ${stretchDepth}\\t check: ${check}`);
    
         const longLivedTree = bottomUpTree(maxDepth);
    
         for (let depth = 4; depth <= maxDepth; depth += 2) {
             const iterations = 1 << maxDepth - depth + 4;
             work(iterations, depth);
         }
    
         console.log(`long lived tree of depth ${maxDepth}\\t check: ${itemCheck(longLivedTree)}`);
     }
    
     function work(iterations, depth) {
         let check = 0;
         for (let i = 0; i < iterations; i++) {
             check += itemCheck(bottomUpTree(depth));
         }
         console.log(`${iterations}\\t trees of depth ${depth}\\t check: ${check}`);
     }
    
     function TreeNode(left, right) {
         return {left, right};
     }
    
     function itemCheck(node) {
         if (node.left === null) {
             return 1;
         }
         return 1 + itemCheck(node.left) + itemCheck(node.right);
     }
    
     function bottomUpTree(depth) {
         return depth > 0
             ? new TreeNode(bottomUpTree(depth - 1), bottomUpTree(depth - 1))
             : new TreeNode(null, null);
     }
    """;

  @Override
  public void setup() {
    super.setup();

    context.eval("nim", NIM_SOURCE);

    context.eval("js", JS_SOURCE);
  }

  @Benchmark
  public int nim_eval() {
    context.eval("nim", "run(" + INPUT + ");");
    return 1;
  }

  @Benchmark
  public int js_eval() {
    context.eval("js", "run(" + INPUT + ");");
    return 1;
  }
}
