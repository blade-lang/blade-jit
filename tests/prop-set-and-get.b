class Node {
  @new(left, right) {
    self.left = left
    self.right = right
  }
}

def make_tree(depth) {
    if  depth > 0 {
        return new Node(make_tree(depth - 1), make_tree(depth - 1))
    }
    return new Node()
}

const g = make_tree(21)
var f = make_tree(21)
echo g
echo f
echo g.left
echo f.left
echo new Node(new Node(), new Node()).left

def c() { if 1 { return new Node(new Node(), new Node()) } else { return new Node()} }
echo c().left
