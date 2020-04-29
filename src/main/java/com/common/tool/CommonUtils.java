package com.common.tool;

import com.google.common.collect.Maps;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class CommonUtils {

    // object to map
    public static Map<String, Object> convertObjectToMap(Object obj, Class clazz) {
        Map<String, Object> ret = Maps.newHashMap();
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            Class<?> type = field.getType();
            if(type.isPrimitive() || String.class == type){
                try{
                    String dstName = field.getName();
                    PropertyDescriptor pd = new PropertyDescriptor(dstName, clazz);
                    Method method = pd.getReadMethod();
                    Object dstObject = method.invoke(obj);
                    ret.put(dstName,  dstObject);
                }catch (Exception ex){
                    continue;
                }
            }
        }
        return ret;
    }
}
