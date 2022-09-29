package test;

public class mergeSortTest {
    public static void main(String[] args) {
        int[] array = {12, 20, 5, 16, 15, 1, 30, 45, 23, 9, 7, 45, 35, 45, 56, 23, 12, 34, 45, 67, 78, 56, 67, 34, 2, 4, 1, 3, 5, 7};
        mergeSort(array);
        printArray(array);
    }

    public static void mergeSort(int[] array) {
        int[] temp = new int[array.length];
        int gap = 1;

        while (gap < array.length) {
            for (int i = 0; i < array.length; i += gap * 2) {
                int mid = i + gap;
                int right = mid + gap;

                if (mid > array.length) {
                    mid = array.length;
                }
                if (right > array.length) {
                    right = array.length;
                }
                mergeData(array, i, mid, right, temp);
            }
            System.arraycopy(temp, 0, array, 0, array.length);
            gap <<= 1;
        }
    }

    // 合并数据  [left,mid)  [mid,right)
    private static void mergeData(int[] array, int left, int mid, int right, int[] temp) {
        int index = left;
        int begin1 = left, begin2 = mid;

        while (begin1 < mid && begin2 < right) {
            if (array[begin1] <= array[begin2]) {
                temp[index++] = array[begin1++];
            } else {
                temp[index++] = array[begin2++];
            }
        }
        // 如果第一个区间中还有数据
        while (begin1 < mid) {
            temp[index++] = array[begin1++];
        }
        // 如果第二个区间有数据
        while (begin2 < right) {
            temp[index++] = array[begin2++];
        }
    }

    public static void printArray(int[] array) {
        for (int e : array) {
            System.out.print(e + " ");
        }
    }
}
