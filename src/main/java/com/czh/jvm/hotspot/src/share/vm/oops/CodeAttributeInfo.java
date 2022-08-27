package com.czh.jvm.hotspot.src.share.vm.oops;

import com.czh.jvm.hotspot.src.share.vm.interpreter.BytecodeStream;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private List<ExceptionItem> exceptionTable = new ArrayList<>();

    // 如局部变量表、操作数栈
    private int attributesCount;

    private Map<String, AttributeInfo> attributes = new HashMap<>();

    @Override
    public String toString() {
        return "CodeAttributeInfo{}";
    }

    public ExceptionItem findExceptionHandle(int current) {
        for (ExceptionItem item: exceptionTable) {
            if (item.getStartPc() <= current && item.getEndPc() >= current) {
                return item;
            }
        }

        return null;
    }

    @Data
    public class ExceptionItem {
        private int startPc;
        private int endPc;
        private int handlerPc;
        private int catchType;
    }
}