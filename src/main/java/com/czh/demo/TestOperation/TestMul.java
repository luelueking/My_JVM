package com.czh.demo.TestOperation;

public class TestMul {
    public static void main(String[] args) {
        mulInt();
        mulDouble();
        mulLong();
        mulFloat();
    }

    private static void mulInt() {
        int a = 2;
        int b = 3;
        System.out.println(a*b);
    }
    private static void mulLong() {
        long a = 2;
        long b = 3;
        System.out.println(a*b);
    }
    private static void mulFloat() {
        float a = 2;
        float b = 3;
        System.out.println(a*b);
    }
    private static void mulDouble() {
        double a = 2;
        double b = 3;
        System.out.println(a*b);
    }
}
