var array = [44, 33, 22, 11]
def bubbleSort(array) {
    iter var i = 0; i < array.length - 1; i = i + 1 {
        iter var j = 0; j < array.length - 1 - i; j = j + 1 {
            if array[j] > array[j + 1] {
                var tmp = array[j]
                array[j] = array[j + 1]
                array[j + 1] = tmp
            }
        }
    }
}

echo array
bubbleSort(array)
echo array
echo array[0]
echo array[1]
echo array[2]
echo array[3]