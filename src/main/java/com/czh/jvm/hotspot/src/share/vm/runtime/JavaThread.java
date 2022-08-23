package com.czh.jvm.hotspot.src.share.vm.runtime;

import lombok.Data;

import java.util.Stack;


/**
 * java线程，也就是虚拟机栈
 */
@Data
public class JavaThread extends Thread {

    private Stack<VFrame> stack = new Stack<>();

}