package com.czh.jvm.hotspot.src.share.vm.runtime;

import com.czh.jvm.hotspot.src.share.tools.DataTranslate;
import com.czh.jvm.hotspot.src.share.vm.utilities.BasicType;
import lombok.Data;


@Data
public class StackValue {

    private int type;

    /**
     * 数据存储在这里的情况：
     *  1、float
     *  2、long
     */
    private byte[] data;

    /**
     * 1、少于4字节，全部用int存储
     * 2、double 类型用二个int存储
     */
    private int val;

    /**
     * 引用类型用Object存储
     */
    private Object object;

    public StackValue(int type, int val) {
        this.type = type;
        this.val = val;
    }

    public StackValue(int type, Object val) {
        this.type = type;
        this.object = val;
    }

    public StackValue(int type, float v) {
        this.type = type;
        this.data = DataTranslate.floatToByte(v);
    }

    public StackValue(int type, long v) {
        this.type = type;
        this.data = DataTranslate.longToBytes(v);
    }
//
    public Object getData() {
        switch (type) {
            case BasicType.T_FLOAT:
                return DataTranslate.byteToFloat(data);
            case BasicType.T_LONG:
                return DataTranslate.bytesToLong(data);
            case BasicType.T_INT:
                return val;
        }

        return null;
    }
}
