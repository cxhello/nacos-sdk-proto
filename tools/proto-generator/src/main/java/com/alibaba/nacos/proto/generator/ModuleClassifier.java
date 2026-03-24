package com.alibaba.nacos.proto.generator;

public class ModuleClassifier {

    public String classify(Class<?> clazz) {
        String pkg = clazz.getPackage().getName();
        if (pkg.contains(".config.remote")) return "config";
        if (pkg.contains(".naming.remote") || pkg.contains(".naming.pojo")) return "naming";
        if (pkg.contains(".lock.remote")) return "lock";
        if (pkg.contains(".ai.remote")) return "ai";
        return "common";
    }

    public String getFileName(Class<?> clazz, String module) {
        String simpleName = clazz.getSimpleName();
        if (module.equals("common")) return "common.proto";
        if (module.equals("lock")) return "lock.proto";

        if (!simpleName.endsWith("Request") && !simpleName.endsWith("Response")) {
            return simpleName.toLowerCase() + ".proto";
        }

        if (simpleName.endsWith("Request")) return module + "_request.proto";
        return module + "_response.proto";
    }
}
