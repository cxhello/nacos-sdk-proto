package com.alibaba.nacos.proto.generator;

import com.alibaba.nacos.api.remote.request.InternalRequest;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.request.ServerRequest;
import com.alibaba.nacos.api.remote.response.Response;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class FieldExtractor {

    private static final Set<Class<?>> STOP_CLASSES = Set.of(
        Object.class, Request.class, Response.class,
        InternalRequest.class, ServerRequest.class
    );

    private static final List<String> REQUEST_BASE_FIELDS = List.of("requestId");
    private static final List<String> RESPONSE_BASE_FIELDS = List.of(
        "resultCode", "errorCode", "message", "requestId"
    );

    public List<FieldInfo> extract(Class<?> clazz) {
        List<FieldInfo> allFields = new ArrayList<>();

        Set<String> seen = new HashSet<>();
        addBaseFields(clazz, allFields, seen);

        List<Class<?>> hierarchy = getHierarchy(clazz);

        // Build a map from field name to the most-derived class that declares it
        // (child class overrides parent if same name exists)
        Map<String, Class<?>> fieldOwnerClass = new LinkedHashMap<>();
        for (int i = hierarchy.size() - 1; i >= 0; i--) {
            for (Field field : hierarchy.get(i).getDeclaredFields()) {
                fieldOwnerClass.put(field.getName(), hierarchy.get(i));
            }
        }

        for (Class<?> level : hierarchy) {
            for (Field field : level.getDeclaredFields()) {
                if (!shouldExclude(field)
                    && fieldOwnerClass.get(field.getName()) == level
                    && seen.add(field.getName())) {
                    allFields.add(new FieldInfo(field.getName(), field.getType(), field.getGenericType()));
                }
            }
        }
        return allFields;
    }

    private void addBaseFields(Class<?> clazz, List<FieldInfo> fields, Set<String> seen) {
        List<String> baseFieldNames;
        Class<?> baseClass;
        if (Response.class.isAssignableFrom(clazz)) {
            baseFieldNames = RESPONSE_BASE_FIELDS;
            baseClass = Response.class;
        } else if (Request.class.isAssignableFrom(clazz)) {
            baseFieldNames = REQUEST_BASE_FIELDS;
            baseClass = Request.class;
        } else {
            // Domain objects (not Request/Response subclasses) have no base fields
            return;
        }

        for (String name : baseFieldNames) {
            try {
                Field f = baseClass.getDeclaredField(name);
                if (seen.add(name)) {
                    fields.add(new FieldInfo(f.getName(), f.getType(), f.getGenericType()));
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
    }

    private List<Class<?>> getHierarchy(Class<?> clazz) {
        List<Class<?>> chain = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && !STOP_CLASSES.contains(current)) {
            chain.add(current);
            current = current.getSuperclass();
        }
        Collections.reverse(chain);
        return chain;
    }

    private boolean shouldExclude(Field field) {
        int mod = field.getModifiers();
        if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) return true;
        if (field.isAnnotationPresent(JsonIgnore.class)) return true;
        if (field.getName().equals("headers")) return true;
        if (field.getName().startsWith("_")) return true;
        return false;
    }
}
