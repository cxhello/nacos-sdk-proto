package com.alibaba.nacos.proto.generator;

import com.alibaba.nacos.api.remote.Payload;

import java.lang.reflect.Modifier;
import java.util.*;

public class ClassScanner {

    public List<Class<?>> scan() {
        ServiceLoader<Payload> payloads = ServiceLoader.load(Payload.class);
        List<Class<?>> classes = new ArrayList<>();
        for (Payload payload : payloads) {
            Class<?> clazz = payload.getClass();
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                classes.add(clazz);
            }
        }
        classes.sort(Comparator.comparing(Class::getSimpleName));
        return classes;
    }

    public String getInheritanceChain(Class<?> clazz) {
        List<String> chain = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class
               && !current.getPackage().getName().startsWith("java.")) {
            chain.add(current.getSimpleName());
            current = current.getSuperclass();
        }
        return String.join(" -> ", chain);
    }
}
