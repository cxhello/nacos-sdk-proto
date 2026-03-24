package com.alibaba.nacos.proto.generator;

public class ModuleClassifier {

    public String classify(Class<?> clazz) {
        // For inner classes, classify based on the enclosing class's package
        Class<?> target = clazz.getEnclosingClass() != null ? clazz.getEnclosingClass() : clazz;
        String pkg = target.getPackage().getName();
        // Match both .config.remote (requests/responses) and .config.model etc. (domain objects)
        if (pkg.contains(".api.config.")) return "config";
        if (pkg.contains(".api.naming.")) return "naming";
        if (pkg.contains(".api.lock.")) return "lock";
        if (pkg.contains(".api.ai.")) return "ai";
        return "common";
    }

    public String getFileName(Class<?> clazz, String module) {
        // Inner classes go to the same file as their enclosing class
        if (clazz.getEnclosingClass() != null) {
            return getFileName(clazz.getEnclosingClass(), classify(clazz.getEnclosingClass()));
        }

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
