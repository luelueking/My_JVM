package com.czh.jvm.hotspot.src.share.vm.oops;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录了添加在类声明、字段声明或方法声明上面，且运行时可见的注解。jvm必须令这些注解可供使用，以便使某些合适的反射API能够把它们返回给调用者
 */
@Data
public class RuntimeVisibleAnnotations extends AttributeInfo {

    private int annotationsNum;
    private List<Annotation> annotations = new ArrayList<>();

    private String attrName;

}
