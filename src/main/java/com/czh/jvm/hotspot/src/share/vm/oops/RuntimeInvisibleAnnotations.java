package com.czh.jvm.hotspot.src.share.vm.oops;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于保存Java语言中标注在类、方法或字段声明上运行时非可见的注解
 */
@Data
public class RuntimeInvisibleAnnotations extends AttributeInfo {

    private int annotationsNum;
    private List<Annotation> annotations = new ArrayList<>();
    private String attrName;

}
