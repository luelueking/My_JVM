package com.czh.jvm.hotspot.src.share.vm.oops;

import com.czh.jvm.hotspot.src.share.vm.classfile.DescriptorStream2;
import com.czh.jvm.hotspot.src.share.vm.utilities.AccessFlags;
import lombok.Data;

@Data
public class MethodInfo {

    private InstanceKlass belongKlass;

    private AccessFlags accessFlags;

    private int nameIndex;
    private int descriptorIndex;
    private int attributesCount;

    private CodeAttributeInfo[] attributes;
    private Exceptions exceptions;

    private String methodName;
    private DescriptorStream2 descriptor;

    public void initAttributeContainer() {
        attributes = new CodeAttributeInfo[attributesCount];
    }

    @Override
    public String toString() {
        return "MethodInfo{ }";
    }
}