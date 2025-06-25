class M {
    m(a) {
        return a + 1
    }
}
def invokeM(target, argument) {
    return target.m(argument)
}
var m = new M()
echo invokeM(m, 13)