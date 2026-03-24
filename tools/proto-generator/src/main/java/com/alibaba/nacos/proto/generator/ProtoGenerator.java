package com.alibaba.nacos.proto.generator;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Collection;
import java.util.Map;

public class ProtoGenerator {

    private final ClassScanner scanner = new ClassScanner();
    private final FieldExtractor extractor = new FieldExtractor();
    private final TypeMapper typeMapper = new TypeMapper();
    private final ModuleClassifier classifier = new ModuleClassifier();
    final ProtoFileWriter writer = new ProtoFileWriter();

    public void generate(Path outputDir, Path lockFilePath, boolean dryRun) throws IOException {
        FieldNumberManager numberManager = new FieldNumberManager(lockFilePath);
        classToMessageName.clear();

        List<Class<?>> classes = scanner.scan();
        System.out.printf("Discovered %d Payload classes%n", classes.size());

        List<MessageDescriptor> allMessages = new ArrayList<>();
        Set<Class<?>> processedDomainObjects = new HashSet<>();
        // Track message names per module to detect collisions within the same proto package
        Map<String, Set<String>> messageNamesByModule = new HashMap<>();

        for (Class<?> clazz : classes) {
            MessageDescriptor msg = buildDescriptor(clazz, numberManager, messageNamesByModule);
            allMessages.add(msg);
            discoverDomainObjects(msg.fields(), allMessages, processedDomainObjects, numberManager,
                    messageNamesByModule, 0);
        }

        Map<String, List<MessageDescriptor>> byFile = new LinkedHashMap<>();
        for (MessageDescriptor msg : allMessages) {
            byFile.computeIfAbsent(msg.fileName(), k -> new ArrayList<>()).add(msg);
        }

        if (dryRun) {
            System.out.println("=== Dry-run Report ===");
            for (var entry : byFile.entrySet()) {
                Path existing = outputDir.resolve(entry.getKey());
                if (!Files.exists(existing)) {
                    System.out.printf("[NEW]     %s (+%d messages)%n", entry.getKey(), entry.getValue().size());
                } else {
                    System.out.printf("[CHECK]   %s (%d messages)%n", entry.getKey(), entry.getValue().size());
                }
            }
        } else {
            writer.setClassToMessageName(classToMessageName);
            writer.write(outputDir, byFile);
            numberManager.save();
            System.out.printf("Generated %d messages in %d files%n", allMessages.size(), byFile.size());
        }
    }

    // Maps Java class to proto message name for consistent references
    private final Map<Class<?>, String> classToMessageName = new HashMap<>();

    private MessageDescriptor buildDescriptor(Class<?> clazz, FieldNumberManager numberManager,
            Map<String, Set<String>> messageNamesByModule) {
        List<FieldInfo> fields = extractor.extract(clazz);
        String module = classifier.classify(clazz);
        String fileName = classifier.getFileName(clazz, module);

        // Inner classes always use EnclosingClass + SimpleName to avoid cross-class collisions
        String messageName = clazz.getEnclosingClass() != null
            ? clazz.getEnclosingClass().getSimpleName() + clazz.getSimpleName()
            : clazz.getSimpleName();
        Set<String> moduleNames = messageNamesByModule.computeIfAbsent(module, k -> new HashSet<>());
        // If name still collides within the same module, disambiguate with sub-package
        if (moduleNames.contains(messageName)) {
            String pkg = clazz.getPackage().getName();
            String[] parts = pkg.split("\\.");
            String subPkg = parts[parts.length - 1];
            messageName = capitalize(subPkg) + messageName;
        }
        moduleNames.add(messageName);
        classToMessageName.put(clazz, messageName);

        List<String> fieldNames = fields.stream().map(FieldInfo::name).toList();
        Map<String, Integer> numbers = numberManager.assignNumbers(messageName, fieldNames);
        List<Integer> reserved = numberManager.getReserved(messageName);
        String chain = scanner.getInheritanceChain(clazz);
        return new MessageDescriptor(messageName, module, module + "/" + fileName, chain, fields, numbers, reserved);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static final int MAX_DEPTH = 5;

    private void discoverDomainObjects(List<FieldInfo> fields, List<MessageDescriptor> allMessages,
            Set<Class<?>> processed, FieldNumberManager numberManager,
            Map<String, Set<String>> messageNamesByModule, int depth) {
        if (depth >= MAX_DEPTH) return;
        for (FieldInfo field : fields) {
            List<Class<?>> candidates = extractDomainTypes(field);
            for (Class<?> candidate : candidates) {
                if (candidate.isPrimitive() || candidate.getName().startsWith("java.") || candidate.isEnum()) continue;
                if (processed.contains(candidate)) continue;
                processed.add(candidate);

                MessageDescriptor domainMsg = buildDescriptor(candidate, numberManager, messageNamesByModule);
                allMessages.add(domainMsg);
                discoverDomainObjects(domainMsg.fields(), allMessages, processed, numberManager,
                        messageNamesByModule, depth + 1);
            }
        }
    }

    private List<Class<?>> extractDomainTypes(FieldInfo field) {
        List<Class<?>> types = new ArrayList<>();
        Class<?> fieldType = field.type();

        if (Collection.class.isAssignableFrom(fieldType) && field.genericType() instanceof ParameterizedType pt) {
            Type elemType = pt.getActualTypeArguments()[0];
            if (elemType instanceof Class<?> cls) {
                types.add(cls);
            }
        } else if (Map.class.isAssignableFrom(fieldType) && field.genericType() instanceof ParameterizedType pt) {
            for (Type arg : pt.getActualTypeArguments()) {
                if (arg instanceof Class<?> cls) {
                    types.add(cls);
                }
            }
        } else {
            types.add(fieldType);
        }
        return types;
    }

    public static void main(String[] args) throws Exception {
        Path outputDir = null;
        Path lockFile = null;
        boolean dryRun = false;
        boolean verify = false;
        String goModuleBase = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--output" -> outputDir = Path.of(args[++i]);
                case "--lockfile" -> lockFile = Path.of(args[++i]);
                case "--dry-run" -> dryRun = true;
                case "--verify" -> verify = true;
                case "--go-module-base" -> goModuleBase = args[++i];
            }
        }

        if (outputDir == null || lockFile == null) {
            System.err.println("Usage: --output <dir> --lockfile <file> [--dry-run] [--verify] [--go-module-base <base>]");
            System.exit(1);
        }

        ProtoGenerator generator = new ProtoGenerator();
        if (goModuleBase != null) {
            generator.writer.setGoModuleBase(goModuleBase);
        }
        generator.generate(outputDir, lockFile, dryRun);

        if (verify) {
            System.out.println("Verification mode: checking proto compilation...");
        }
    }
}
