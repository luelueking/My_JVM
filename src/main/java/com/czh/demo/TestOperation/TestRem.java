package com.czh.demo.TestOperation;

public class TestRem {
    public static void main(String[] args) {
        remInt();
        remFloat();
        remLong();
        remDouble();
    }

    private static void remInt() {
        int a = 7;
        int b = 5;
        System.out.println(a%b);
    }

    private static void remLong() {
        long a = 7;
        long b = 5;
        System.out.println(a%b);
    }

    private static void remFloat() {
        float a = 7;
        float b = 5;
        System.out.println(a%b);
    }

    private static void remDouble() {
        double a = 7;
        double b = 5;
        System.out.println(a%b);
    }
}
