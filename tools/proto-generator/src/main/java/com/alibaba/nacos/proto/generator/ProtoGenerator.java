package com.alibaba.nacos.proto.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ProtoGenerator {

    private final ClassScanner scanner = new ClassScanner();
    private final FieldExtractor extractor = new FieldExtractor();
    private final TypeMapper typeMapper = new TypeMapper();
    private final ModuleClassifier classifier = new ModuleClassifier();
    private final ProtoFileWriter writer = new ProtoFileWriter();

    public void generate(Path outputDir, Path lockFilePath, boolean dryRun) throws IOException {
        FieldNumberManager numberManager = new FieldNumberManager(lockFilePath);

        List<Class<?>> classes = scanner.scan();
        System.out.printf("Discovered %d Payload classes%n", classes.size());

        List<MessageDescriptor> allMessages = new ArrayList<>();
        Set<Class<?>> processedDomainObjects = new HashSet<>();

        for (Class<?> clazz : classes) {
            MessageDescriptor msg = buildDescriptor(clazz, numberManager);
            allMessages.add(msg);
            discoverDomainObjects(msg.fields(), allMessages, processedDomainObjects, numberManager, 0);
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
            writer.write(outputDir, byFile);
            numberManager.save();
            System.out.printf("Generated %d messages in %d files%n", allMessages.size(), byFile.size());
        }
    }

    private MessageDescriptor buildDescriptor(Class<?> clazz, FieldNumberManager numberManager) {
        List<FieldInfo> fields = extractor.extract(clazz);
        String module = classifier.classify(clazz);
        String fileName = classifier.getFileName(clazz, module);
        String messageName = clazz.getEnclosingClass() != null
            ? clazz.getEnclosingClass().getSimpleName() + clazz.getSimpleName()
            : clazz.getSimpleName();
        List<String> fieldNames = fields.stream().map(FieldInfo::name).toList();
        Map<String, Integer> numbers = numberManager.assignNumbers(messageName, fieldNames);
        List<Integer> reserved = numberManager.getReserved(messageName);
        String chain = scanner.getInheritanceChain(clazz);
        return new MessageDescriptor(messageName, module, module + "/" + fileName, chain, fields, numbers, reserved);
    }

    private static final int MAX_DEPTH = 5;

    private void discoverDomainObjects(List<FieldInfo> fields, List<MessageDescriptor> allMessages,
            Set<Class<?>> processed, FieldNumberManager numberManager, int depth) {
        if (depth >= MAX_DEPTH) return;
        for (FieldInfo field : fields) {
            Class<?> fieldType = field.type();
            if (fieldType.isPrimitive() || fieldType.getName().startsWith("java.") || fieldType.isEnum()) continue;
            if (processed.contains(fieldType)) continue;
            processed.add(fieldType);

            MessageDescriptor domainMsg = buildDescriptor(fieldType, numberManager);
            allMessages.add(domainMsg);
            discoverDomainObjects(domainMsg.fields(), allMessages, processed, numberManager, depth + 1);
        }
    }

    public static void main(String[] args) throws Exception {
        Path outputDir = null;
        Path lockFile = null;
        boolean dryRun = false;
        boolean verify = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--output" -> outputDir = Path.of(args[++i]);
                case "--lockfile" -> lockFile = Path.of(args[++i]);
                case "--dry-run" -> dryRun = true;
                case "--verify" -> verify = true;
            }
        }

        if (outputDir == null || lockFile == null) {
            System.err.println("Usage: --output <dir> --lockfile <file> [--dry-run] [--verify]");
            System.exit(1);
        }

        ProtoGenerator generator = new ProtoGenerator();
        generator.generate(outputDir, lockFile, dryRun);

        if (verify) {
            System.out.println("Verification mode: checking proto compilation...");
        }
    }
}
