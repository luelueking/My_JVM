package com.czh.demo.TestLambda;

public class TestLambda {
    public static void main(String[] args) {
        MyLambda01 myLambda01 = () -> {
            System.out.println("Hello Lambda !");
        };
        myLambda01.run();

        MyLambda02 myLambda02 = (x,y) -> {
            System.out.println("X:"+x+",Y:"+y);
        };
        myLambda02.run(1,2);
    }
}
