package com.czh.jvm.hotspot.src.share.vm.prims;


import com.czh.jvm.hotspot.src.share.vm.interpreter.BytecodeInterpreter;
import com.czh.jvm.hotspot.src.share.vm.oops.CodeAttributeInfo;
import com.czh.jvm.hotspot.src.share.vm.oops.InstanceKlass;
import com.czh.jvm.hotspot.src.share.vm.oops.MethodInfo;
import com.czh.jvm.hotspot.src.share.vm.runtime.JavaThread;
import com.czh.jvm.hotspot.src.share.vm.runtime.JavaVFrame;
import com.czh.jvm.hotspot.src.share.vm.runtime.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地方法调用接口
 */
public class JavaNativeInterface {

    private final static Logger logger = LoggerFactory.getLogger(JavaNativeInterface.class);

    public static InstanceKlass findClass(String name) {
        return null;
    }


    public static MethodInfo getMethodID(InstanceKlass klass, String name, String descriptorName) {
        MethodInfo[] methods = klass.getMethods();
        for (MethodInfo method:methods) {
            String tmpName = (String) klass.getConstantPool().getDataMap().get(method.getNameIndex());
            String tmpDescriptor = (String) klass.getConstantPool().getDataMap().get(method.getDescriptorIndex());

            if (tmpName.equals(name) && tmpDescriptor.equals(descriptorName)) {
                logger.info("找到了方法: " + name + "#" + descriptorName);

                return method;
            }
        }

        logger.error("没有找到方法: " + name + "#" + descriptorName);

        return null;
    }

    /**
     * 执行静态方法
     * @param method
     */
    public static void callStaticMethod(MethodInfo method) {
        JavaThread thread = Threads.currentThread();

        if (!method.getAccessFlags().isStatic()) {
            throw new Error("只能调用静态方法");
        }

        CodeAttributeInfo codeAttributeInfo = method.getAttributes()[0];

        // 创建栈帧
        JavaVFrame frame = new JavaVFrame(codeAttributeInfo.getMaxLocals(), method);

        thread.getStack().push(frame);

        logger.info("第 " + thread.getStack().size() + " 个栈帧");

        // 执行任务交给字节码解释器
        BytecodeInterpreter.run(thread, method);
    }

}
