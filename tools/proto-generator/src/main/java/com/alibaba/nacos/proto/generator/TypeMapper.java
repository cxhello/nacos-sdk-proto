package com.alibaba.nacos.proto.generator;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

public class TypeMapper {

    public String mapType(Class<?> javaType, Type genericType) {
        if (javaType == String.class) return "string";
        if (javaType == int.class || javaType == Integer.class) return "int32";
        if (javaType == long.class || javaType == Long.class) return "int64";
        if (javaType == boolean.class || javaType == Boolean.class) return "bool";
        if (javaType == double.class || javaType == Double.class) return "double";
        if (javaType == float.class || javaType == Float.class) return "float";
        if (javaType.isEnum()) return "string";
        if (javaType == Object.class || javaType == Serializable.class) {
            return "google.protobuf.Value";
        }

        if (Map.class.isAssignableFrom(javaType) && genericType instanceof ParameterizedType pt) {
            String keyType = resolveTypeArg(pt.getActualTypeArguments()[0]);
            String valType = resolveTypeArg(pt.getActualTypeArguments()[1]);
            return "map<" + keyType + ", " + valType + ">";
        }

        if (Collection.class.isAssignableFrom(javaType) && genericType instanceof ParameterizedType pt) {
            String elemType = resolveTypeArg(pt.getActualTypeArguments()[0]);
            return "repeated " + elemType;
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
