package com.czh.jvm.hotspot.src.share.vm.oops;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InstanceKlass extends Klass{
    //魔数
    private byte[] magic = new byte[4];
    //次版本号
    private byte[] minorVersion = new byte[2];
    //主版本号
    private byte[] majorVersion = new byte[2];

    //常量池
    private ConstantPool constantPool;

    private int accessFlag;//类访问控制权限
    private int thisClass;//类名
    private int superClass;//父类名

    private int interfacesLength;//接口数量
    private List<InterfaceInfo> interfaceInfos = new ArrayList<>();

    private int fieldsLength;//成员属性数量
    private List<FieldInfo> fields = new ArrayList<>();

    private int methodLength;//成员方法数量
    private MethodInfo[] methods;

    private int attributeLength;//类属性数量
    private List<AttributeInfo> attributeInfos = new ArrayList<>();

    public InstanceKlass() {
        constantPool = new ConstantPool();

        constantPool.setKlass(this);
    }

    public void initMethodsContainer() {
        methods = new MethodInfo[methodLength];
    }

    @Override
    public String toString() {
        return "InstanceKlass{ }";
    }
}
