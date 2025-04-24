catch {
  var a = 20
  raise new Error('Something happened')
} /*as e {
  echo e
}*/ then {
  echo 'Cleaning up'
}