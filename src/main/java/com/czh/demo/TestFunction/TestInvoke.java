package com.czh.demo.TestFunction;

public class TestInvoke {
    public static void main(String[] args) {
//        invokeSpecial();
//        invokeStatic();
        invokeVirtual();
    }

    public static void invokeStatic() {
        test();
    }

    public static void invokeVirtual() {
        new TestInvoke().show();
    }

    public static void invokeSpecial() {
        new TestInvoke().hello();
    }

    public void show() {
        System.out.println("show");
    }

    private void hello() {
        System.out.println("hello");
    }

    public static void test() {
        System.out.println("test");
    }
}
