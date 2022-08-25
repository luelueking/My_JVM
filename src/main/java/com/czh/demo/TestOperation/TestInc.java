package com.czh.demo.TestOperation;

public class TestInc {
    public static void main(String[] args) {
        IncInt1();
        IncInt2();
        IncFloat1();
        IncFloat2();
        IncLong1();
        IncLong2();
        IncDouble1();
        IncDouble2();
    }

    private static void IncInt1() {
        int i = 1;
        System.out.println(i++);
    }

    private static void IncInt2() {
        int i = 1;
        System.out.println(++i);
    }

    private static void IncFloat1() {
        float i = 1;
        System.out.println(i++);
    }

    private static void IncFloat2() {
        float i = 1;
        System.out.println(++i);
    }

    private static void IncLong1() {
        long i = 1;
        System.out.println(i++);
    }

    private static void IncLong2() {
        long i = 1;
        System.out.println(++i);
    }

    private static void IncDouble1() {
        double i = 1;
        System.out.println(i++);
    }

    private static void IncDouble2() {
        double i = 1;
        System.out.println(++i);
    }
}
