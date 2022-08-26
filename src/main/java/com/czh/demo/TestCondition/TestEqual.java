package com.czh.demo.TestCondition;

public class TestEqual {
    public static void main(String[] args) {
        byteEqual();
        longEqual();
        ObjectEqual();
    }

    private static void ObjectEqual() {
        Object obj = null;
        System.out.println(obj == null);
    }


    private static void longEqual() {
        long v1 = 10;
        long v2 = 20;
        System.out.println(v1 != v2);
        System.out.println(v1 == v2);
    }

    private static void byteEqual() {
        byte v1 = 10;
        byte v2 = 20;
        System.out.println(v1 != v2);
        System.out.println(v1 == v2);
    }

}
