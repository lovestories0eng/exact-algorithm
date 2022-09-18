package test;

import java.util.HashSet;
import java.util.Set;

// 测试HashSet是否能够存储相同元素
public class HashSetTest {
    public static void main(String[] args) {
        Set<Integer> set = new HashSet<>();
        set.add(1);
        set.add(1);
        System.out.println("Break point!");
        // 结论：HashSet无法存储相同元素
    }
}
