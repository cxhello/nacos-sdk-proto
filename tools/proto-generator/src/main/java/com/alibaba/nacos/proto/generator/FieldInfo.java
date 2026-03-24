package com.alibaba.nacos.proto.generator;

import java.lang.reflect.Type;

public record FieldInfo(String name, Class<?> type, Type genericType) {
}
