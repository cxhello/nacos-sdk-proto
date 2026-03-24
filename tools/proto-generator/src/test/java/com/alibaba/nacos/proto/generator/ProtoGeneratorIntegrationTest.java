package com.alibaba.nacos.proto.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtoGeneratorIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testGenerateProducesProtoFiles() throws Exception {
        Path outputDir = tempDir.resolve("proto");
        Path lockFile = tempDir.resolve("field-numbers.json");

        ProtoGenerator generator = new ProtoGenerator();
        generator.generate(outputDir, lockFile, false);

        assertTrue(Files.exists(outputDir.resolve("common/common.proto")));
        assertTrue(Files.exists(outputDir.resolve("config/config_request.proto")));
        assertTrue(Files.exists(outputDir.resolve("config/config_response.proto")));
        assertTrue(Files.exists(outputDir.resolve("naming/naming_request.proto")));
        assertTrue(Files.exists(outputDir.resolve("naming/naming_response.proto")));

        assertTrue(Files.exists(lockFile));

        String configReq = Files.readString(outputDir.resolve("config/config_request.proto"));
        assertTrue(configReq.contains("message ConfigQueryRequest"));
        assertTrue(configReq.contains("string requestId = 1;"));
        assertTrue(configReq.contains("string dataId = 2;"));
        assertTrue(configReq.contains("string tag = 5;"));
        assertTrue(configReq.contains("package nacos.config;"));
    }

    @Test
    void testDryRunDoesNotWriteFiles() throws Exception {
        Path outputDir = tempDir.resolve("proto");
        Path lockFile = tempDir.resolve("field-numbers.json");

        ProtoGenerator generator = new ProtoGenerator();
        generator.generate(outputDir, lockFile, true);

        assertFalse(Files.exists(outputDir.resolve("config/config_request.proto")));
        assertFalse(Files.exists(lockFile));
    }

    @Test
    void testIdempotent() throws Exception {
        Path outputDir = tempDir.resolve("proto");
        Path lockFile = tempDir.resolve("field-numbers.json");

        ProtoGenerator generator = new ProtoGenerator();
        generator.generate(outputDir, lockFile, false);
        String firstRun = Files.readString(outputDir.resolve("config/config_request.proto"));

        generator.generate(outputDir, lockFile, false);
        String secondRun = Files.readString(outputDir.resolve("config/config_request.proto"));

        assertEquals(firstRun, secondRun);
    }
}
