package com.czh.demo.TestArray;

public class TestArray {

    public static void main(String[] args) {
        test3();
    }

    public static void test1() {
        int[] arr = new int[3];

        arr[0] = 10;

        System.out.println(arr[0]);
    }

    public static void test2() {
        int[] arr = {1, 2, 3};

        System.out.println(arr[0]);
    }

    public static void test3() {
        Object[] objects = new Object[3];

        objects[0] = new TestArray();

        System.out.println(objects[0]);
    }
}