package com.czh.jvm.hotspot.src.share.vm.oops;

import com.czh.jvm.hotspot.src.share.vm.interpreter.BytecodeStream;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CodeAttributeInfo {

    private int attrNameIndex;
    private int attrLength;

    //操作数栈最大深度
    private int maxStack;
    //局部变量最大槽数
    private int maxLocals;

    private int codeLength;
    private BytecodeStream code;

    private int exceptionTableLength;

    // 如局部变量表、操作数栈
    private int attributesCount;

    private Map<String, AttributeInfo> attributes = new HashMap<>();

}