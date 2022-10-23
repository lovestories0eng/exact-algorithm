package test;

import java.util.ArrayList;

public class ArrayListTest {
    public static void main(String[] args) {
        ArrayList<Integer> arr = new ArrayList<>();
        arr.add(0);
        arr.add(1);
        arr.add(2);
        arr.add(3);
        printArrayList(arr);

        arr.add(0, 6);
        printArrayList(arr);

        arr.add(2, 9);
        printArrayList(arr);
    }

    public static void printArrayList(ArrayList<Integer> arr) {
        for (Integer integer : arr) {
            System.out.printf("%d ", integer);
        }
        System.out.println();
    }
}
