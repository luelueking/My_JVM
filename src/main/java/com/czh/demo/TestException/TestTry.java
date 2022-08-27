package com.czh.demo.TestException;

public class TestTry {
    public static void main(String[] args) {
        try {
            int a = 5 / 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
