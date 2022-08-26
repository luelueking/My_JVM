package com.czh.demo.TestLoop;

public class TestFor {

    public static void main(String[] args) {
        hardFor();
    }

    public static void normalFor() {
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
        }
    }


    public static void hardFor() {
        byte[] arr = {1, 2};

        for (byte b: arr) {
            System.out.println(b);
        }
    }

    public static void normalFor2() {
        byte[] arr = {1, 2};

        for (int i = 0; i < arr.length; i++) {
            System.out.println(arr[i]);
        }
    }

}