package com.alibaba.nacos.proto.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FieldNumberManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testFirstRunGeneratesLockFile() throws Exception {
        Path lockFile = tempDir.resolve("field-numbers.json");
        FieldNumberManager manager = new FieldNumberManager(lockFile);

        Map<String, Integer> numbers = manager.assignNumbers("ConfigQueryRequest",
            List.of("requestId", "dataId", "group", "tenant", "tag"));

        assertEquals(1, numbers.get("requestId"));
        assertEquals(2, numbers.get("dataId"));
        assertEquals(5, numbers.get("tag"));

        manager.save();
        assertTrue(Files.exists(lockFile));
    }

    @Test
    void testExistingNumbersPreserved() throws Exception {
        Path lockFile = tempDir.resolve("field-numbers.json");
        FieldNumberManager manager1 = new FieldNumberManager(lockFile);
        manager1.assignNumbers("Test", List.of("a", "b", "c"));
        manager1.save();

        FieldNumberManager manager2 = new FieldNumberManager(lockFile);
        Map<String, Integer> numbers = manager2.assignNumbers("Test", List.of("a", "b", "c", "d"));

        assertEquals(1, numbers.get("a"));
        assertEquals(2, numbers.get("b"));
        assertEquals(3, numbers.get("c"));
        assertEquals(4, numbers.get("d"));
    }

    @Test
    void testDeletedFieldBecomesReserved() throws Exception {
        Path lockFile = tempDir.resolve("field-numbers.json");
        FieldNumberManager manager1 = new FieldNumberManager(lockFile);
        manager1.assignNumbers("Test", List.of("a", "b", "c"));
        manager1.save();

        FieldNumberManager manager2 = new FieldNumberManager(lockFile);
        Map<String, Integer> numbers = manager2.assignNumbers("Test", List.of("a", "c"));

        assertEquals(1, numbers.get("a"));
        assertEquals(3, numbers.get("c"));
        assertNull(numbers.get("b"));
        assertTrue(manager2.getReserved("Test").contains(2));
    }
}
