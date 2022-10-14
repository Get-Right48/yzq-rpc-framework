package rpc.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;

public class JsonUtil {
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static <T> byte[] serialize(T obj){
        byte[] bytes = new byte[0];
        try {
            bytes = objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static <T> T deserialize(byte[] bytes,Class<T> cls){
        T obj = null;
        try {
            obj = objectMapper.readValue(bytes, cls);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static <T> T jsonToObject(String json,Class<?> cls){
        T obj = null;
        JavaType type = objectMapper.getTypeFactory().constructType(cls);
        try {
            obj = objectMapper.readValue(json,type);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static String objectToJson(Object obj){
        String json = "";
        try {
            json = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static <T> T jsonToObjectList(String json,
                                               Class<?> collectionClass, Class<?>... elementClass) {
        T obj = null;
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(
                collectionClass, elementClass);
        try {
            obj = objectMapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }

    public static <T> T jsonToObjectHashMap(String json,
                                                  Class<?> keyClass, Class<?> valueClass) {
        T obj = null;
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(HashMap.class, keyClass, valueClass);
        try {
            obj = objectMapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }
}
