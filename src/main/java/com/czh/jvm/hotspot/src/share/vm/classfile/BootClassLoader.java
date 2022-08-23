package com.czh.jvm.hotspot.src.share.vm.classfile;

import cn.hutool.core.io.FileUtil;
import com.czh.jvm.hotspot.src.share.vm.oops.InstanceKlass;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 根类加载器
 */
public class BootClassLoader {
    public static final String SUFFIX = ".class";

    //类加载器的加载路径
    private static String searchPath = "/Users/zhchen/Downloads/My_JVM_Source/target/classes/";


    //用于存储该类加载器加载的所有类
    private static Map<String, InstanceKlass> classLoaderData = new HashMap<>();


    //main函数所在的类在此保存一份引用，方便快速定位到，实际中hotspot源码并没有这么设计，我这里是为了简便
    private static InstanceKlass mainKlass = null;

    //加载Main类通过调用LoadKlass方法
    public static InstanceKlass loadMainKlass(String name) {
        if (null != mainKlass) {
            return mainKlass;
        }

        return loadKlass(name);
    }

    public static InstanceKlass loadKlass(String name) {
        return loadKlass(name, true);
    }

    //根据类名加载方法
    public static InstanceKlass loadKlass(String name, Boolean resolve) {
        //从classLoaderData找到方法的名字
        InstanceKlass klass = findLoadedKlass(name);
        if (null != klass) {
            return klass;
        }

        //解析字节码
        klass = readAndParse(name);

        if (resolve) {
            resolveKlass();
        }

        return klass;
    }

    private static InstanceKlass readAndParse(String name) {
        String tmpName = name.replace('.', '/');
        String filePath = searchPath + tmpName + SUFFIX;

        // 读取字节码文件
        byte[] content = FileUtil.readBytes(new File(filePath));

        // 解析字节码文件
        InstanceKlass klass = ClassFileParser.parseClassFile(content);

        // 存入
        classLoaderData.put(name, klass);

        return klass;
    }

    public static InstanceKlass findLoadedKlass(String name) {
        return classLoaderData.get(name);
    }

    private static void resolveKlass() {
    }

    public static void setMainKlass(InstanceKlass mainKlass) {
        BootClassLoader.mainKlass = mainKlass;
    }
}
