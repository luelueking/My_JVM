package com.czh.jvm.hotspot.src.share.vm.interpreter;

import com.czh.jvm.hotspot.src.share.vm.classfile.BootClassLoader;
import com.czh.jvm.hotspot.src.share.vm.classfile.DescriptorStream2;
import com.czh.jvm.hotspot.src.share.vm.memory.StackObj;
import com.czh.jvm.hotspot.src.share.vm.oops.ArrayOop;
import com.czh.jvm.hotspot.src.share.vm.oops.ConstantPool;
import com.czh.jvm.hotspot.src.share.vm.oops.InstanceKlass;
import com.czh.jvm.hotspot.src.share.vm.oops.MethodInfo;
import com.czh.jvm.hotspot.src.share.vm.prims.JavaNativeInterface;
import com.czh.jvm.hotspot.src.share.vm.runtime.JavaThread;
import com.czh.jvm.hotspot.src.share.vm.runtime.JavaVFrame;
import com.czh.jvm.hotspot.src.share.vm.runtime.StackValue;
import com.czh.jvm.hotspot.src.share.vm.utilities.BasicType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 字节码解释器
 */
public class BytecodeInterpreter extends StackObj {

    private static Logger logger = LoggerFactory.getLogger(BytecodeInterpreter.class);

    public static void run(JavaThread thread, MethodInfo method) {
        // 得到字节码指令
        BytecodeStream code = method.getAttributes()[0].getCode();

        // 得到栈帧
        JavaVFrame frame = (JavaVFrame) thread.getStack().peek();

        int c;

        while (!code.end()) {
            c = code.getU1Code();

            switch (c) {
                case Bytecodes.LDC: { //从运行时常量池中提取数据并压入操作数栈
                    logger.info("执行指令: LDC");

                    // 取出操作数
                    int operand = code.getU1Code();

                    // 取出常量池中的信息
                    int tag = method.getBelongKlass().getConstantPool().getTag()[operand];
                    switch (tag) {
                        case ConstantPool.JVM_CONSTANT_Float: {
                            // 取出数值
                            float f = (float) method.getBelongKlass().getConstantPool().getDataMap().get(operand);
                            // 压入栈
                            frame.getStack().push(new StackValue(BasicType.T_FLOAT, f));
                            break;
                        }
                        case ConstantPool.JVM_CONSTANT_String: {
                            int index = (int) method.getBelongKlass().getConstantPool().getDataMap().get(operand);

                            String content = (String) method.getBelongKlass().getConstantPool().getDataMap().get(index);

                            //压栈
                            frame.getStack().push(new StackValue(BasicType.T_OBJECT, content));

                            break;
                        }
                        case ConstantPool.JVM_CONSTANT_Class: {
                            break;
                        }
                        default: {
                            logger.error("未知类型");

                            break;
                        }
                    }

                    break;
                }
                case Bytecodes.LDC2_W:{ // 从运行时常量池中提取long或者double数据并压人操作数栈
                    logger.info("执行指令: LDC2_W");

                    // 取出操作数
                    int operand = code.getUnsignedShort();

                    /**
                     * 数值入栈，这边实现方式略有差别
                     *      long是用8字节的byte数组存储的
                     *      double是用两个slot存储的
                     */
                    int tag = method.getBelongKlass().getConstantPool().getTag()[operand];

                    if (ConstantPool.JVM_CONSTANT_Long == tag) {
                        long l = (long) method.getBelongKlass().getConstantPool().getDataMap().get(operand);
                        frame.getStack().push(new StackValue(BasicType.T_LONG, l));
                    } else if (ConstantPool.JVM_CONSTANT_Double == tag) {
                        double d = (double) method.getBelongKlass().getConstantPool().getDataMap().get(operand);
                        frame.getStack().pushDouble(d);
                    } else {
                        throw new Error("无法识别的格式");
                    }

                    break;

                }
                case Bytecodes.RETURN: { // 方法中返回void
                    logger.info("执行指令: RETURN");

                    // pop出栈帧
                    thread.getStack().pop();

                    logger.info("\t 剩余栈帧数量: " + thread.getStack().size());

                    break;
                }
                case Bytecodes.GETSTATIC: { //获取类的静态字段值
                    logger.info("执行指令: GETSTATIC");

                    // 获取操作数
                    short operand = code.getUnsignedShort();

                    String className = method.getBelongKlass().getConstantPool().getClassNameByFieldInfo(operand);
                    String fieldName = method.getBelongKlass().getConstantPool().getFieldName(operand);

                    try {
                        Class<?> clazz = Class.forName(className.replace('/', '.'));

                        Field field = clazz.getField(fieldName);

                        frame.getStack().push(new StackValue(BasicType.T_OBJECT, field.get(null)));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case Bytecodes.INVOKESTATIC: { //调用类静态方法
                    logger.info("执行指令：INVOKESTATIC");

                    // 取出操作数
                    short operand = code.getUnsignedShort();

                    // 获取类名
                    String className = method.getBelongKlass().getConstantPool().getClassNameByMethodInfo(operand);
                    String methodName = method.getBelongKlass().getConstantPool().getMethodNameByMethodInfo(operand);
                    String descriptorName = method.getBelongKlass().getConstantPool().getDescriptorNameByMethodInfo(operand);

                    if (className.startsWith("java")) {
                        DescriptorStream2 descriptorStream = new DescriptorStream2(descriptorName);
                        descriptorStream.parseMethod();

                        Object[] params = descriptorStream.getParamsVal(frame);
                        Class[] paramsClass = descriptorStream.getParamsType();

                        try {
                            Class clazz = Class.forName(className.replace('/', '.'));

                            Method fun = clazz.getMethod(methodName, paramsClass);

                            /**
                             * 处理：
                             *  1、无返回值
                             *  2、有返回值
                             */
                            if (BasicType.T_VOID == descriptorStream.getReturnElement().getType()) {
                                fun.invoke(clazz, params);
                            } else {
                                descriptorStream.pushField(fun.invoke(clazz, params), frame);
                            }
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        InstanceKlass klass = BootClassLoader.findLoadedKlass(className.replace('/', '.'));
                        if (null == klass) {
                            logger.info("\t 开始加载未加载的类:" + className);

                            klass = BootClassLoader.loadKlass(className.replace('/', '.'));
                        }

                        MethodInfo methodID = JavaNativeInterface.getMethodID(klass, methodName, descriptorName);
                        if (null == methodID) {
                            throw new Error("不存在的方法: " + methodName + "#" + descriptorName);
                        }

                        // 不然方法重复调用会出错。因为程序计数器上次执行完指向的是尾部
                        methodID.getAttributes()[0].getCode().reset();

                        // 调用
                        JavaNativeInterface.callStaticMethod(methodID);
                    }
                    break;
                }
                case Bytecodes.INVOKEVIRTUAL: { // 调用实例方法
                    logger.info("执行指令: INVOKEVIRTUAL");

                    // 取出操作数
                    short operand = code.getUnsignedShort();

                    // 获取类名、方法名、方法描述符
                    String className = method.getBelongKlass().getConstantPool().getClassNameByMethodInfo(operand);
                    String methodName = method.getBelongKlass().getConstantPool().getMethodNameByMethodInfo(operand);
                    String descriptorName = method.getBelongKlass().getConstantPool().getDescriptorNameByMethodInfo(operand);

                    /**
                     * 判断是系统类还是自定义的类
                     *  系统类走反射
                     *  自定义的类自己处理
                     */
                    if (className.startsWith("java")) {
                        DescriptorStream2 descriptorStream = new DescriptorStream2(descriptorName);
                        descriptorStream.parseMethod();

                        Object[] params = descriptorStream.getParamsVal(frame);
                        Class[] paramsClass = descriptorStream.getParamsType();

                        Object obj = frame.getStack().pop().getObject();

                        try {
                            Method fun = obj.getClass().getMethod(methodName, paramsClass);

                            /**
                             * 处理：
                             *  1、无返回值
                             *  2、有返回值
                             */
                            if (BasicType.T_VOID == descriptorStream.getReturnElement().getType()) {
                                fun.invoke(obj, params);
                            } else {
                                descriptorStream.pushField(fun.invoke(obj, params), frame);
                            }
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    } else {

                    }

                    break;
                }

                case Bytecodes.BALOAD:{ // 从数组中读取一个byte或者boolean数据
                    logger.info("执行指令: BALOAD");

                    int index = frame.getStack().pop().getVal();
                    ArrayOop oop = frame.getStack().popArray(frame);

                    if (index > oop.getSize() - 1) {
                        throw new Error("数组访问越界");
                    }

                    int v = (int) oop.getData().get(index);

                    frame.getStack().pushInt(v, frame);

                    break;
                }
                case Bytecodes.BASTORE:{ // 从操作数栈读取一个byte或boolean类型数据并存入数组中
                    logger.info("执行指令: BASTORE");

                    int val = frame.getStack().pop().getVal();
                    int index = frame.getStack().pop().getVal();
                    ArrayOop oop = (ArrayOop) frame.getStack().pop().getObject();

                    if (index > oop.getSize() - 1) {
                        throw new Error("数组访问越界");
                    }

                    try {
                        oop.getData().get(index);

                        oop.getData().set(index, val);
                    } catch (Exception e) {
                        oop.getData().add(val);
                    }

                    break;

                }
                case Bytecodes.ARRAYLENGTH:{ // 取数组长度
                    logger.info("执行指令：ARRARYLENGTH");

                    ArrayOop o = frame.getStack().popArray(frame);

                    frame.getStack().pushInt(o.getSize(), frame);

                    break;
                }
                case Bytecodes.NEWARRAY:{ // 创建一个新数组
                    logger.info("执行指令: NEWARRAY");

                    int arrSize = frame.getStack().pop().getVal();

                    int arrType = code.getU1Code();

                    ArrayOop array = new ArrayOop(arrType, arrSize);

                    frame.getStack().pushArray(array, frame);

                    break;
                }
                case Bytecodes.IF_ICMPGE: { // int数值的条件分支判断
                    logger.info("执行指令: IF_ICMPGE");

                    // 取出比较数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 取出操作数
                    short operand = code.getUnsignedShort();

                    // 基本验证
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 比较
                    if (value2.getVal() >= value1.getVal()) {
                        /**
                         * -1：减去IF_ICMPEQ指令占用的1B
                         * -2：减去操作数operand占用的2B
                         *
                         * 因为跳转的位置是从该条指令的起始位置开始算的
                         */
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.IF_ICMPGT: {
                    logger.info("执行指令: IF_ICMPGT");

                    // 取出比较数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 取出操作数
                    short operand = code.getUnsignedShort();

                    // 基本验证
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 比较
                    if (value2.getVal() > value1.getVal()) {
                        /**
                         * -1：减去IF_ICMPEQ指令占用的1B
                         * -2：减去操作数operand占用的2B
                         *
                         * 因为跳转的位置是从该条指令的起始位置开始算的
                         */
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.IF_ICMPLE: {
                    logger.info("执行指令: IF_ICMPLE");

                    // 取出比较数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 取出操作数
                    short operand = code.getUnsignedShort();

                    // 基本验证
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 比较
                    if (value2.getVal() <= value1.getVal()) {
                        /**
                         * -1：减去IF_ICMPEQ指令占用的1B
                         * -2：减去操作数operand占用的2B
                         *
                         * 因为跳转的位置是从该条指令的起始位置开始算的
                         */
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.IFNONNULL:{ // 引用不为空的条件分支判断
                    logger.info("执行指令: IFNONNULL");

                    // 取出操作数
                    short operand = code.getUnsignedShort();

                    Object o = frame.getStack().pop().getData();

                    // 比较
                    if (null != o) {
                        code.inc(operand - 1 - 2);
                    }
                    break;
                }
                case Bytecodes.ALOAD_0:{ // 从局部变量表加载一个reference类型值到操作数栈中
                    logger.info("执行指令: ALOAD_0");
                    // 从局部变量表取出数据
                    StackValue value = frame.getLocals().get(0);
                    // 压入栈
                    frame.getStack().push(value);
                    break;
                }
                case Bytecodes.ALOAD_1:{ // 从局部变量表加载一个reference类型值到操作数栈中
                    logger.info("执行指令: ALOAD_1");
                    // 从局部变量表取出数据
                    StackValue value = frame.getLocals().get(1);
                    // 压入栈
                    frame.getStack().push(value);
                    break;
                }
                case Bytecodes.ASTORE_0:{ // 将一个reference类型的数据保存到本地变量表中
                    logger.info("执行指令: ASTORE_0");
                    // 取出数据
                    StackValue value = frame.getStack().pop();
                    // 存入局部变量表
                    frame.getLocals().add(0, value);
                    break;
                }
                case Bytecodes.ASTORE_1:{ // 将一个reference类型的数据保存到本地变量表中
                    logger.info("执行指令: ASTORE_1");
                    // 取出数据
                    StackValue value = frame.getStack().pop();
                    // 存入局部变量表
                    frame.getLocals().add(1, value);
                    break;
                }
                case Bytecodes.ACONST_NULL:{ // 将一个null值入栈到操作数栈中
                    logger.info("执行指令: ACONST_NULL");
                    frame.getStack().pushNull(frame);
                    break;
                }
                case Bytecodes.IFEQ: {
                    logger.info("执行指令: IFEQ");

                    int i = (int) frame.getStack().pop().getData();
                    int operand = code.getUnsignedShort();

                    if (0 == i) {
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.IFNE:{ // 整数与0比较的条件分支判断
                    logger.info("执行指令: IFNE");

                    int i = (int) frame.getStack().pop().getData();
                    int operand = code.getUnsignedShort();

                    if (0 != i) {
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.IFLT: {
                    logger.info("执行指令: IFLT");

                    int i = (int) frame.getStack().pop().getData();
                    int operand = code.getUnsignedShort();

                    if (i < 0) {
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.IFGE: {
                    logger.info("执行指令: IFGE");

                    int i = (int) frame.getStack().pop().getData();
                    int operand = code.getUnsignedShort();

                    if (i >= 0) {
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.IFGT: {
                    logger.info("执行指令: IFGT");

                    int i = (int) frame.getStack().pop().getData();
                    int operand = code.getUnsignedShort();

                    if (i > 0) {
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.IFLE: {
                    logger.info("执行指令: IFLE");

                    int i = (int) frame.getStack().pop().getData();
                    int operand = code.getUnsignedShort();

                    if (i <= 0) {
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.LCMP:{ // 比较二个long类型数据的大小
                    logger.info("执行指令: LCMP");

                    long l1 = (long) frame.getStack().pop().getData();
                    long l2 = (long) frame.getStack().pop().getData();

                    if (l1 > l2) {
                        frame.getStack().pushInt(1, frame);
                    } else if (l1 == l2) {
                        frame.getStack().pushInt(0, frame);
                    } else if (l1 < l2) {
                        frame.getStack().pushInt(-1, frame);
                    }

                    break;
                }
                case Bytecodes.IF_ICMPNE:{ // int数值的条件分之判断!=
                    logger.info("执行指令: IF_ICMPNE");

                    // 取出比较数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 取出操作数
                    short operand = code.getUnsignedShort();

                    // 基本验证
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 比较
                    if (value1.getVal() != value2.getVal()) {
                        /**
                         * -1：减去IF_ICMPEQ指令占用的1B
                         * -2：减去操作数operand占用的2B
                         *
                         * 因为跳转的位置是从该条指令的起始位置开始算的
                         */
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.GOTO:{ // 无条件分支跳转
                    logger.info("执行指令: GOTO");

                    short operand = code.getUnsignedShort();

                    code.inc(operand - 2 - 1);

                    break;
                }
                case Bytecodes.IF_ICMPEQ:{ // int数值的条件分之判断 ==
                    logger.info("执行指令: IF_ICMPEQ");

                    // 取出比较数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 取出操作数
                    short operand = code.getUnsignedShort();

                    // 基本验证
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 比较,当相等时需要跳转
                    if (value1.getVal() == value2.getVal()) {
                        /**
                         * -1：减去IF_ICMPEQ指令占用的1B
                         * -2：减去操作数operand占用的2B
                         *
                         * 因为跳转的位置是从该条指令的起始位置开始算的
                         */
                        code.inc(operand - 1 - 2);
                    }

                    break;
                }
                case Bytecodes.I2L: {
                    logger.info("执行指令: I2L");

                    int v = (int) frame.getStack().pop().getData();
                    long l = v;

                    StackValue value = new StackValue(BasicType.T_LONG, l);

                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.I2F: {
                    logger.info("执行指令: I2F");

                    int v = (int) frame.getStack().pop().getData();
                    float f = v;

                    StackValue value = new StackValue(BasicType.T_FLOAT, f);

                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.I2D: {
                    logger.info("执行指令: I2D");

                    int value = frame.getStack().pop().getVal();
                    double v = value;

                    frame.getStack().pushDouble(v);

                    break;
                }
                case Bytecodes.L2I: {
                    logger.info("执行指令: L2I");

                    long l = (long) frame.getStack().pop().getData();
                    int i = (int) l;

                    frame.getStack().pushInt(i, frame);

                    break;
                }
                case Bytecodes.L2F: {
                    logger.info("执行指令: L2F");

                    long l = (long) frame.getStack().pop().getData();
                    float f = l;

                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, f));

                    break;
                }
                case Bytecodes.L2D: {
                    logger.info("执行指令: L2D");

                    long l = (long) frame.getStack().pop().getData();
                    double d = l;

                    frame.getStack().pushDouble(d);

                    break;
                }
                case Bytecodes.F2I: {
                    logger.info("执行指令: F2I");

                    float f = (float) frame.getStack().pop().getData();
                    int i = (int) f;

                    frame.getStack().pushInt(i, frame);

                    break;
                }
                case Bytecodes.F2L: {
                    logger.info("执行指令: F2L");

                    float f = (float) frame.getStack().pop().getData();
                    long v = (long) f;

                    frame.getStack().push(new StackValue(BasicType.T_LONG, v));

                    break;
                }
                case Bytecodes.F2D: {
                    logger.info("执行指令: F2D");

                    float f = (float) frame.getStack().pop().getData();
                    double v = f;

                    frame.getStack().pushDouble(v);

                    break;
                }
                case Bytecodes.D2I: {
                    logger.info("执行指令: D2I");

                    double d = frame.getStack().popDouble();
                    int v = (int) d;

                    frame.getStack().pushInt(v, frame);

                    break;
                }
                case Bytecodes.D2L: {
                    logger.info("执行指令: D2L");

                    double d = frame.getStack().popDouble();
                    long v = (long) d;

                    frame.getStack().push(new StackValue(BasicType.T_LONG, v));

                    break;
                }
                case Bytecodes.D2F: {
                    logger.info("执行指令: D2F");

                    double d = frame.getStack().popDouble();
                    float v = (float) d;

                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, v));

                    break;
                }
                case Bytecodes.I2B: {
                    logger.info("执行指令: I2B");

                    int i = (int) frame.getStack().pop().getData();
                    byte v = (byte) i;

                    frame.getStack().pushInt(v, frame);

                    break;
                }
                case Bytecodes.I2C: {
                    logger.info("执行指令: I2C");

                    int i = (int) frame.getStack().pop().getData();
                    char v = (char) i;

                    frame.getStack().pushInt(v, frame);

                    break;
                }
                case Bytecodes.I2S: {
                    logger.info("执行指令: I2S");

                    int i = (int) frame.getStack().pop().getData();
                    short v = (short) i;

                    frame.getStack().pushInt(v, frame);

                    break;
                }
                case Bytecodes.DUP:{ // 复制操作数栈栈顶的值，并插入到栈顶
                    logger.info("执行指令: DUP");

                    // 取出栈顶元素
                    StackValue value = frame.getStack().peek();

                    // 压入栈
                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.DUP2: { // 复制栈顶一个long或double类型的数据
                    logger.info("执行指令: DUP2");

                    StackValue value = frame.getStack().peek();

                    /**
                     * 需要通过栈顶元素来判断是long还是double
                     * long只需要取一次
                     * double需要取两次
                     *  stack用的是C++ STL库中的，无法取第二个元素，所以这块处理麻烦些，直接取出double数值，两次压栈
                     */
                    if (BasicType.T_DOUBLE == value.getType()) {
                        double d = frame.getStack().popDouble();

                        frame.getStack().pushDouble(d);
                        frame.getStack().pushDouble(d);
                    } else if (BasicType.T_LONG == value.getType()){
                        frame.getStack().push(value);
                    } else {
                        throw new Error("无法识别的类型");
                    }

                    break;
                }
                case Bytecodes.IINC:{ // 以常数为变量的局部变量自增
                    logger.info("执行指令: IINC");

                    // 第一个操作数：slot的index
                    int index = code.getU1Code();

                    // 第二个操作数：增加多少
                    int step = code.getU1Code2();

                    // 完成运算
                    int v = (int) frame.getLocals().get(index).getData();
                    v += step;

                    // 写回局部变量表
                    frame.getLocals().add(index, new StackValue(BasicType.T_INT, v));

                    break;
                }
                case Bytecodes.DREM:{ // double类型相除
                    double v1 = frame.getStack().popDouble();
                    double v2 = frame.getStack().popDouble();
                    double ret = v2 % v1;

                    logger.info("执行指令: DDIV， 结果: " + ret);

                    frame.getStack().pushDouble(ret);
                    break;
                }
                case Bytecodes.LREM:{ // long类型相除
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_LONG || value2.getType() != BasicType.T_LONG) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    long ret = (long)value2.getData() % (long)value1.getData();

                    logger.info("执行指令: LREM，运行结果: " + ret);

                    frame.getStack().push(new StackValue(BasicType.T_LONG, ret));

                    break;

                }
                case Bytecodes.FREM:{ // float类型相除
                    //取出操作数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_FLOAT || value2.getType() != BasicType.T_FLOAT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }
                    // 运算
                    float ret = (float)value2.getData() % (float) value1.getData();

                    logger.info("执行指令: FREM，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, ret));

                    break;
                }
                case Bytecodes.IREM:{ // int类型相除
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 运算
                    int ret = (int)value2.getData() % (int)value1.getData();

                    logger.info("执行指令: IREM，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_INT, ret));

                    break;
                }
                case Bytecodes.DDIV:{ // double类型相除
                    double v1 = frame.getStack().popDouble();
                    double v2 = frame.getStack().popDouble();
                    double ret = v2 / v1;

                    logger.info("执行指令: DDIV， 结果: " + ret);

                    frame.getStack().pushDouble(ret);
                    break;
                }
                case Bytecodes.LDIV:{ // long类型相除
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_LONG || value2.getType() != BasicType.T_LONG) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    long ret = (long)value2.getData() / (long)value1.getData();

                    logger.info("执行指令: LDIV，运行结果: " + ret);

                    frame.getStack().push(new StackValue(BasicType.T_LONG, ret));

                    break;

                }
                case Bytecodes.FDIV:{ // float类型相除
                    //取出操作数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_FLOAT || value2.getType() != BasicType.T_FLOAT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }
                    // 运算
                    float ret = (float)value2.getData() / (float) value1.getData();

                    logger.info("执行指令: FDIV，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, ret));

                    break;
                }
                case Bytecodes.IDIV:{ // int类型相除
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 运算
                    int ret = (int)value2.getData() / (int)value1.getData();

                    logger.info("执行指令: IDIV，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_INT, ret));

                    break;
                }
                case Bytecodes.DMUL:{ // double类型数据相加
                    double v1 = frame.getStack().popDouble();
                    double v2 = frame.getStack().popDouble();
                    double ret = v1 * v2;

                    logger.info("执行指令: DMUL， 结果: " + ret);

                    frame.getStack().pushDouble(ret);
                    break;
                }
                case Bytecodes.FMUL:{ // float类型相乘
                    //取出操作数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_FLOAT || value2.getType() != BasicType.T_FLOAT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }
                    // 运算
                    float ret = (float)value1.getData() * (float) value2.getData();

                    logger.info("执行指令: FMUL，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, ret));

                    break;
                }
                case Bytecodes.LMUL:{ // long类型相乘
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_LONG || value2.getType() != BasicType.T_LONG) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    long ret = (long)value1.getData() * (long)value2.getData();

                    logger.info("执行指令: LMUL，运行结果: " + ret);

                    frame.getStack().push(new StackValue(BasicType.T_LONG, ret));

                    break;

                }
                case Bytecodes.IMUL:{ // int类型相乘
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 运算
                    int ret = (int)value1.getData() * (int)value2.getData();

                    logger.info("执行指令: IMUL，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_INT, ret));

                    break;
                }
                case Bytecodes.DSUB:{ // double类型相减
                    double v1 = frame.getStack().popDouble();
                    double v2 = frame.getStack().popDouble();
                    double ret = v2 - v1;

                    logger.info("执行指令: DSUB， 结果: " + ret);

                    frame.getStack().pushDouble(ret);
                    break;
                }
                case Bytecodes.LSUB:{ // long类型相减
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_LONG || value2.getType() != BasicType.T_LONG) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    long ret = (long)value2.getData() - (long)value1.getData();

                    logger.info("执行指令: LSUB，运行结果: " + ret);

                    frame.getStack().push(new StackValue(BasicType.T_LONG, ret));

                    break;

                }
                case Bytecodes.FSUB:{ // float类型相减
                    //取出操作数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_FLOAT || value2.getType() != BasicType.T_FLOAT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }
                    // 运算
                    float ret = (float)value2.getData() - (float) value1.getData();

                    logger.info("执行指令: FSUB，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, ret));

                    break;
                }
                case Bytecodes.ISUB:{ //int类型数据相减
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 运算
                    int ret = (int)value2.getData() - (int)value1.getData();

                    logger.info("执行指令: ISUB，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_INT, ret));

                    break;

                }
                case Bytecodes.DADD:{ // double类型数据相加
                    double v1 = frame.getStack().popDouble();
                    double v2 = frame.getStack().popDouble();
                    double ret = v1 + v2;

                    logger.info("执行指令: DADD， 结果: " + ret);

                    frame.getStack().pushDouble(ret);
                    break;
                }
                case Bytecodes.LADD:{ // long类型数据相加
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_LONG || value2.getType() != BasicType.T_LONG) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    long ret = (long)value1.getData() + (long)value2.getData();

                    logger.info("执行指令: LADD，运行结果: " + ret);

                    frame.getStack().push(new StackValue(BasicType.T_LONG, ret));

                    break;

                }
                case Bytecodes.FADD:{ //float类型数据相加
                    //取出操作数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_FLOAT || value2.getType() != BasicType.T_FLOAT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }
                    // 运算
                    float ret = (float)value1.getData() + (float) value2.getData();

                    logger.info("执行指令: FADD，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, ret));

                    break;
                }
                case Bytecodes.IADD:{ // int类型数据相加
                    logger.info("执行指令：IADD");
                    // 取出操作数
                    StackValue value1 = frame.getStack().pop();
                    StackValue value2 = frame.getStack().pop();

                    // 检查操作数类型
                    if (value1.getType() != BasicType.T_INT || value2.getType() != BasicType.T_INT) {
                        logger.error("不匹配的数据类型");

                        throw new Error("不匹配的数据类型");
                    }

                    // 运算
                    int ret = (int)value1.getData() + (int)value2.getData();

                    logger.info("执行指令: IADD，运行结果: " + ret);

                    // 压入栈
                    frame.getStack().push(new StackValue(BasicType.T_INT, ret));

                    break;

                }
                case Bytecodes.DCONST_0:{ // 把double类型入栈到操作数栈中
                    logger.info("执行指令：DCONST_0");
                    frame.getStack().pushDouble(0);
                    break;
                }
                case Bytecodes.DCONST_1:{ // 把double类型入栈到操作数栈中
                    logger.info("执行指令：DCONST_1");
                    frame.getStack().pushDouble(1);
                    break;
                }
                case Bytecodes.LCONST_0:{ // 把long类型数据入栈到操作数栈中
                    logger.info("执行指令: LCONST_0");
                    /**
                     * 这里一定要强转成long，否则会当成int处理
                     */
                    frame.getStack().push(new StackValue(BasicType.T_LONG, (long) 0));
                    break;
                }
                case Bytecodes.LCONST_1:{ // 把long类型数据入栈到操作数栈中
                    logger.info("执行指令: LCONST_1");
                    /**
                     * 这里一定要强转成long，否则会当成int处理
                     */
                    frame.getStack().push(new StackValue(BasicType.T_LONG, (long) 1));
                    break;
                }
                case Bytecodes.FCONST_0:{ // 将float数据类型入栈操作数栈中
                    logger.info("执行指令：FCONST_0");
                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, 0f));
                    break;
                }
                case Bytecodes.FCONST_1:{ // 将float数据类型入栈操作数栈中
                    logger.info("执行指令：FCONST_1");
                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, 1f));
                    break;
                }
                case Bytecodes.FCONST_2:{ // 将float数据类型入栈操作数栈中
                    logger.info("执行指令：FCONST_2");
                    frame.getStack().push(new StackValue(BasicType.T_FLOAT, 2f));
                    break;
                }
                case Bytecodes.FLOAD_0:{ // 从局部变量表加载一个float类型值到操作数栈中
                    logger.info("执行指令: FLOAD_0");

                    // 取出局部变量表中的数据
                    StackValue value = frame.getLocals().get(0);

                    // 压入栈
                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.FLOAD_1:{ // 从局部变量表加载一个float类型值到操作数栈中
                    logger.info("执行指令: FLOAD_1");

                    // 取出局部变量表中的数据
                    StackValue value = frame.getLocals().get(1);

                    // 压入栈
                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.FLOAD_2:{ // 从局部变量表加载一个float类型值到操作数栈中
                    logger.info("执行指令: FLOAD_2");

                    // 取出局部变量表中的数据
                    StackValue value = frame.getLocals().get(2);

                    // 压入栈
                    frame.getStack().push(value);

                    break;
                }

                case Bytecodes.FSTORE_0:{ // 将一个float数据保存到本地变量表中
                    logger.info("执行指令：FSTORE_0");
                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(0, value);
                    break;
                }

                case Bytecodes.FSTORE_1:{ // 将一个float数据保存到本地变量表中
                    logger.info("执行指令：FSTORE_1");
                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(1, value);
                    break;
                }
                case Bytecodes.FSTORE_2:{ // 讲一个float数据保存到本地变量表中
                    logger.info("执行指令：FSTORE_2");
                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(2, value);
                    break;
                }
                case Bytecodes.LLOAD_0:{ // 从局部变量表中加载一个long类型值到操作数栈中
                    logger.info("执行指令：LLOAD_0");

                    // 取出局部变量表中的数据
                    StackValue value = frame.getLocals().get(0);

                    // 压入栈
                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.LLOAD_1: {
                    logger.info("执行指令: LLOAD_1");

                    // 取出局部变量表中的数据
                    StackValue value = frame.getLocals().get(1);

                    // 压入栈
                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.LLOAD_2: {
                    logger.info("执行指令: LLOAD_2");

                    // 取出局部变量表中的数据
                    StackValue value = frame.getLocals().get(2);

                    // 压入栈
                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.LLOAD_3: {
                    logger.info("执行指令: LLOAD_3");

                    // 取出局部变量表中的数据
                    StackValue value = frame.getLocals().get(3);

                    // 压入栈
                    frame.getStack().push(value);

                    break;
                }
                case Bytecodes.LSTORE_0:{ // 将一个long类型的数据保存到本地变量表中
                    logger.info("执行命令：LSTORE_0");

                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(0, value);

                    break;
                }
                case Bytecodes.LSTORE_1:{ // 将一个long类型的数据保存到本地变量表中
                    logger.info("执行命令：LSTORE_1");

                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(1, value);

                    break;
                }
                case Bytecodes.LSTORE_2: {
                    logger.info("执行指令: LSTORE_2");

                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(2, value);

                    break;
                }
                case Bytecodes.LSTORE_3: {
                    logger.info("执行指令: LSTORE_3");

                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(3, value);

                    break;
                }
                case Bytecodes.DLOAD:{ // 从局部变量表中加载一个double类型值到操作数栈中
                    logger.info("执行命令：DLOAD");
                    int index = code.getU1Code();

                    StackValue value1 = frame.getLocals().get(index);
                    StackValue value2 = frame.getLocals().get(index+1);

                    frame.getStack().push(value1);
                    frame.getStack().push(value2);
                    break;
                }
                case Bytecodes.DLOAD_0:{ // 从局部变量表加载一个double类型值到操作数栈中
                    logger.info("执行命令：DLOAD_0");
                    // 取出数据
                    StackValue value1 = frame.getLocals().get(0);
                    StackValue value2 = frame.getLocals().get(1);

                    // 压入栈
                    frame.getStack().push(value1);
                    frame.getStack().push(value2);
                    break;
                }
                case Bytecodes.DLOAD_1:{ // 从局部变量表加载一个double类型值到操作数栈中
                    logger.info("执行命令：DLOAD_1");
                    // 取出数据
                    StackValue value1 = frame.getLocals().get(1);
                    StackValue value2 = frame.getLocals().get(2);

                    // 压入栈
                    frame.getStack().push(value1);
                    frame.getStack().push(value2);
                    break;
                }
                case Bytecodes.DLOAD_2:{ // 从局部变量表加载一个double类型值到操作数栈中
                    logger.info("执行命令：DLOAD_2");
                    // 取出数据
                    StackValue value1 = frame.getLocals().get(2);
                    StackValue value2 = frame.getLocals().get(3);

                    // 压入栈
                    frame.getStack().push(value1);
                    frame.getStack().push(value2);
                    break;
                }
                case Bytecodes.DSTORE:{
                    logger.info("执行指令: DSTORE");
                    // 获取操作数
                    int index = code.getU1Code();

                    // 取出数据
                    StackValue[] values = frame.getStack().popDouble2();

                    // 存入局部变量表
                    frame.getLocals().add(index, values[1]);
                    frame.getLocals().add(index+1, values[0]);

                    break;
                }
                case Bytecodes.DSTORE_0: { // 将一个double类型数据保存到本地变量表中，其中0和1必须是指向当前栈帧局部变量表的索引值
                    logger.info("执行指令: DSTORE_0");

                    // 取出数据
                    StackValue[] values = frame.getStack().popDouble2();

                    // 存入局部变量表
                    frame.getLocals().add(0, values[1]);
                    frame.getLocals().add(1, values[0]);

                    break;
                }
                case Bytecodes.DSTORE_1: { // 将一个double类型数据保存到本地变量表中，其中1和2必须是指向当前栈帧局部变量表的索引值
                    logger.info("执行指令: DSTORE_1");

                    // 取出数据
                    StackValue[] values = frame.getStack().popDouble2();

                    // 存入局部变量表
                    frame.getLocals().add(1, values[1]);
                    frame.getLocals().add(2, values[0]);

                    break;
                }
                case Bytecodes.DSTORE_2: { // 将一个double类型数据保存到本地变量表中，其中2和3必须是指向当前栈帧局部变量表的索引值
                    logger.info("执行指令: DSTORE_2");

                    // 取出数据
                    StackValue[] values = frame.getStack().popDouble2();

                    // 存入局部变量表
                    frame.getLocals().add(2, values[1]);
                    frame.getLocals().add(3, values[0]);

                    break;
                }
                case Bytecodes.ILOAD:{ // 从局部变量表加载一个int类型值到操作数栈中
                    logger.info("执行指令：ILOAD");
                    int index = code.getU1Code();

                    StackValue value = frame.getLocals().get(index);

                    frame.getStack().push(value);
                    break;
                }
                case Bytecodes.ILOAD_0:{ // 从局部变量表加载一个int类型值到操作数栈中
                    logger.info("执行指令：ILOAD_0");

                    StackValue value = frame.getLocals().get(0);

                    // 压入栈
                    frame.getStack().push(value);
                    break;
                }
                case Bytecodes.ILOAD_1:{ // 从局部变量表加载一个int类型值到操作数栈中
                    logger.info("执行指令：ILOAD_1");

                    StackValue value = frame.getLocals().get(1);

                    // 压入栈
                    frame.getStack().push(value);
                    break;
                }
                case Bytecodes.ILOAD_2:{ // 从局部变量表加载一个int类型值到操作数栈中
                    logger.info("执行指令：ILOAD_2");

                    StackValue value = frame.getLocals().get(2);

                    // 压入栈
                    frame.getStack().push(value);
                    break;
                }
                case Bytecodes.ILOAD_3:{ // 从局部变量表加载一个int类型值到操作数栈中
                    logger.info("执行指令：ILOAD_3");

                    StackValue value = frame.getLocals().get(3);

                    // 压入栈
                    frame.getStack().push(value);
                    break;
                }
                case Bytecodes.ICONST_0:{ // 将int类型常量入栈到操作数栈中
                    logger.info("执行指令：ICONST_0");
                    frame.getStack().push(new StackValue(BasicType.T_INT, 0));
                    break;
                }
                case Bytecodes.ICONST_1:{ // 将int类型常量入栈到操作数栈中
                    logger.info("执行指令：ICONST_1");
                    frame.getStack().push(new StackValue(BasicType.T_INT, 1));
                    break;
                }
                case Bytecodes.ICONST_2:{ // 将int类型常量入栈到操作数栈中
                    logger.info("执行指令：ICONST_2");
                    frame.getStack().push(new StackValue(BasicType.T_INT, 2));
                    break;
                }
                case Bytecodes.ICONST_3:{
                    logger.info("执行指令：ICONST_3");
                    frame.getStack().push(new StackValue(BasicType.T_INT, 3));
                    break;
                }
                case Bytecodes.ICONST_4:{
                    logger.info("执行指令：ICONST_4");
                    frame.getStack().push(new StackValue(BasicType.T_INT, 4));
                    break;
                }
                case Bytecodes.ICONST_5:{
                    logger.info("执行指令：ICONST_5");
                    frame.getStack().push(new StackValue(BasicType.T_INT, 5));
                    break;
                }
                case Bytecodes.ISTORE: { // 将int类型数据保存到本地变量表中,index是一个无符号byte类型整数，指向当前栈帧局部变量表的索引值
                    logger.info("执行指令: ISTORE");

                    // 获取操作数
                    int index = code.getU1Code();

                    // 取出数据
                    StackValue values = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(index, values);

                    break;
                }
                case Bytecodes.ISTORE_0:{ // 将int类型数据保存到本地变量表中,后面的0代表指向当前栈帧的索引值
                    logger.info("执行指令：ISTORE_0");
                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(0, value);

                    break;
                }
                case Bytecodes.ISTORE_1:{ // 将int类型数据保存到本地变量表中,后面的1代表指向当前栈帧的索引值
                    logger.info("执行指令：ISTORE_1");
                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(1, value);

                    break;
                }
                case Bytecodes.ISTORE_2:{ // 将int类型数据保存到本地变量表中,后面的2代表指向当前栈帧的索引值
                    logger.info("执行指令：ISTORE_2");
                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(2, value);

                    break;
                }
                case Bytecodes.ISTORE_3:{ // 将int类型数据保存到本地变量表中,后面的3代表指向当前栈帧的索引值
                    logger.info("执行指令：ISTORE_3");
                    // 取出栈顶元素
                    StackValue value = frame.getStack().pop();

                    // 存入局部变量表
                    frame.getLocals().add(3, value);

                    break;
                }

                case Bytecodes.BIPUSH:{ // 将一个byte类型入栈
                    logger.info("执行指令：BIPPUSH");
                    // 获取操作数
                    int val = code.getU1Code();
                    // 立刻将byte类型带符号扩展为一个int类型的值value，然后将value入栈到操作数栈中
                    frame.getStack().push(new StackValue(BasicType.T_INT,val));
                    break;
                }

                default: {
                    throw new Error("无效指令"+c);
                }
            } /* end switch */
        } /* end while */
    }

}