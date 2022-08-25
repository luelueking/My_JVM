package com.czh.demo.TestOperation;

public class TestDiv {
    public static void main(String[] args) {
        divInt();
        divFloat();
        divLong();
        divDouble();
    }

    private static void divInt() {
        int a = 4;
        int b = 2;
        System.out.println(a/b);
    }
    private static void divLong() {
        long a = 4;
        long b = 2;
        System.out.println(a/b);
    }
    private static void divFloat() {
        float a = 4;
        float b = 2;
        System.out.println(a/b);
    }
    private static void divDouble() {
        double a = 4;
        double b = 2;
        System.out.println(a/b);
    }


}
