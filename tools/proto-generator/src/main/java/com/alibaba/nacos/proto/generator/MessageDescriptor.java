package com.alibaba.nacos.proto.generator;

import java.util.List;
import java.util.Map;

public record MessageDescriptor(
    String name,
    String module,
    String fileName,
    String inheritanceChain,
    List<FieldInfo> fields,
    Map<String, Integer> fieldNumbers,
    List<Integer> reserved
) {
}
