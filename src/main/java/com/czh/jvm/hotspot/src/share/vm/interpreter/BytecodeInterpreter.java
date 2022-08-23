package com.czh.jvm.hotspot.src.share.vm.interpreter;

import com.czh.jvm.hotspot.src.share.vm.classfile.DescriptorStream2;
import com.czh.jvm.hotspot.src.share.vm.memory.StackObj;
import com.czh.jvm.hotspot.src.share.vm.oops.ConstantPool;
import com.czh.jvm.hotspot.src.share.vm.oops.MethodInfo;
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
                case Bytecodes.INVOKEVIRTUAL: { //调用实例方法
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