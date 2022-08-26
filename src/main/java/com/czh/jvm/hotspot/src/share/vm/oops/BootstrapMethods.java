package com.czh.jvm.hotspot.src.share.vm.oops;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于保存由invokedynamic指令引用的引导方法限定符
 */
@Data
public class BootstrapMethods extends AttributeInfo {

    private String attrName;

    private int numBootstrapMethods;
    private List<Item> bootstrapMethods = new ArrayList<>();

    @Data
    public class Item {
        private int bootstrapMethodRef;// 对应一个CONSTANT_MethodHandle_info的索引
        private int numBootstrapArguments;// bootstrap_arguments数组元素的个数
        private int[] bootstrapArguments;// 常量池的有效索引

        public void initContainter() {
            bootstrapArguments = new int[numBootstrapArguments];
        }
    }

}