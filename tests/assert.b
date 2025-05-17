try {
  assert 5 == 5
  assert [] == [1], 'Non empty list expected'
} catch e {
  echo e.stacktrace
}
