package com.alibaba.nacos.proto.generator;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

public class TypeMapper {

    private Map<Class<?>, String> classToMessageName;

    public void setClassToMessageName(Map<Class<?>, String> classToMessageName) {
        this.classToMessageName = classToMessageName;
    }

    public String mapType(Class<?> javaType, Type genericType) {
        if (javaType == String.class) return "string";
        if (javaType == int.class || javaType == Integer.class) return "int32";
        if (javaType == long.class || javaType == Long.class) return "int64";
        if (javaType == boolean.class || javaType == Boolean.class) return "bool";
        if (javaType == double.class || javaType == Double.class) return "double";
        if (javaType == float.class || javaType == Float.class) return "float";
        if (javaType.isEnum()) return "string";
        if (Throwable.class.isAssignableFrom(javaType)) return "string";
        if (javaType == Object.class || javaType == Serializable.class) {
            return "google.protobuf.Value";
        }

        if (Map.class.isAssignableFrom(javaType) && genericType instanceof ParameterizedType pt) {
            Type keyArg = pt.getActualTypeArguments()[0];
            Type valArg = pt.getActualTypeArguments()[1];
            String keyType = resolveTypeArg(keyArg);
            String valType = resolveTypeArg(valArg);
            // proto3 map values cannot be repeated/map — fall back to Value
            if (valType.startsWith("repeated ") || valType.startsWith("map<")) {
                return "map<" + keyType + ", google.protobuf.Value>";
            }
            return "map<" + keyType + ", " + valType + ">";
        }

        if (Collection.class.isAssignableFrom(javaType) && genericType instanceof ParameterizedType pt) {
            Type elemArg = pt.getActualTypeArguments()[0];
            String elemType = resolveTypeArg(elemArg);
            // proto3 does not allow repeated map — fall back to Value
            if (elemType.startsWith("map<")) {
                return "repeated google.protobuf.Value";
            }
            return "repeated " + elemType;
        }

        // Use registered message name if available (handles name collisions)
        if (classToMessageName != null && classToMessageName.containsKey(javaType)) {
            return classToMessageName.get(javaType);
        }
        return javaType.getSimpleName();
    }

    private String resolveTypeArg(Type typeArg) {
        if (typeArg instanceof Class<?> cls) {
            return mapType(cls, null);
        }
        if (typeArg instanceof WildcardType wt) {
            Type[] upperBounds = wt.getUpperBounds();
            if (upperBounds.length > 0) {
                return resolveTypeArg(upperBounds[0]);
            }
        }
        if (typeArg instanceof ParameterizedType pt) {
            return mapType((Class<?>) pt.getRawType(), typeArg);
        }
        return "string";
    }
}
