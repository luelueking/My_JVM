package com.czh.jvm.hotspot.src.share.vm.oops;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 指出一个方法可能抛出的受检异常
 */
@Data
public class Exceptions {
    private int attrNameIndex;
    private int attrLength;
    private int exceptionsNum;
    private List<Integer> exceptions = new ArrayList<>();
}
