package com.czh.jvm.hotspot.src.share.vm.runtime;

import com.czh.jvm.hotspot.src.share.vm.oops.MethodInfo;
import lombok.Data;

/**
 * 栈帧
 */
@Data
public class JavaVFrame extends VFrame {

    //局部变量表
    private StackValueCollection locals;

    //操作数栈
    private StackValueCollection stack = new StackValueCollection();

    private MethodInfo ownerMethod;

    public JavaVFrame(int maxLocals) {
        locals = new StackValueCollection(maxLocals);
    }

    public JavaVFrame(int maxLocals, MethodInfo methodInfo) {
        locals = new StackValueCollection(maxLocals);

        ownerMethod = methodInfo;
    }
}