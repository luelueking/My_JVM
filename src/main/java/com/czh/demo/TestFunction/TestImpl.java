package com.czh.demo.TestFunction;

public class TestImpl implements Test{
    public static void main(String[] args) {
        new TestImpl().test();
    }
    @Override
    public void test() {
        System.out.println("Test Invoke Interface!");
    }
}
