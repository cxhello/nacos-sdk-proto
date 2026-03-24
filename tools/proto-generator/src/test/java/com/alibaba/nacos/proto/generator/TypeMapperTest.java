package com.alibaba.nacos.proto.generator;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TypeMapperTest {

    private final TypeMapper mapper = new TypeMapper();

    @Test
    void testPrimitiveTypes() {
        assertEquals("string", mapper.mapType(String.class, null));
        assertEquals("int32", mapper.mapType(int.class, null));
        assertEquals("int32", mapper.mapType(Integer.class, null));
        assertEquals("int64", mapper.mapType(long.class, null));
        assertEquals("int64", mapper.mapType(Long.class, null));
        assertEquals("bool", mapper.mapType(boolean.class, null));
        assertEquals("bool", mapper.mapType(Boolean.class, null));
        assertEquals("double", mapper.mapType(double.class, null));
        assertEquals("double", mapper.mapType(Double.class, null));
        assertEquals("float", mapper.mapType(float.class, null));
    }

    @Test
    void testEnumType() {
        assertEquals("string", mapper.mapType(Thread.State.class, null));
    }

    @Test
    void testNestedPojo() {
        assertEquals("Instance", mapper.mapType(
            com.alibaba.nacos.api.naming.pojo.Instance.class, null));
    }

    @Test
    void testMapType() throws Exception {
        var field = TestMapHolder.class.getDeclaredField("stringMap");
        assertEquals("map<string, string>", mapper.mapType(field.getType(), field.getGenericType()));
    }

    @Test
    void testListType() throws Exception {
        var field = TestMapHolder.class.getDeclaredField("stringList");
        assertEquals("repeated string", mapper.mapType(field.getType(), field.getGenericType()));
    }

    @Test
    void testSetType() throws Exception {
        var field = TestMapHolder.class.getDeclaredField("stringSet");
        assertEquals("repeated string", mapper.mapType(field.getType(), field.getGenericType()));
    }

    static class TestMapHolder {
        Map<String, String> stringMap;
        List<String> stringList;
        Set<String> stringSet;
    }
}
