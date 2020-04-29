package com.common.tool.future;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sun.misc.BASE64Encoder;

import java.beans.PropertyDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FutureUtils {

    public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture.thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.<T>toList()));
    }

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