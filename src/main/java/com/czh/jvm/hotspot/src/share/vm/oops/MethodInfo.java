package com.czh.jvm.hotspot.src.share.vm.oops;

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

    private String methodName;

    public void initAttributeContainer() {
        attributes = new CodeAttributeInfo[attributesCount];
    }

    @Override
    public String toString() {
        return "MethodInfo{ "
                + belongKlass.getConstantPool().getMethodName(nameIndex) + "#"
                + belongKlass.getConstantPool().getDescriptorName(descriptorIndex)
                + " }";
    }
}