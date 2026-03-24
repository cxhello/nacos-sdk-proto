package com.alibaba.nacos.proto.generator;

import com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest;
import com.alibaba.nacos.api.naming.remote.request.InstanceRequest;
import com.alibaba.nacos.api.remote.request.ConnectionSetupRequest;
import com.alibaba.nacos.api.remote.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleClassifierTest {

    private final ModuleClassifier classifier = new ModuleClassifier();

    @Test
    void testConfigModule() {
        assertEquals("config", classifier.classify(ConfigQueryRequest.class));
    }

    @Test
    void testNamingModule() {
        assertEquals("naming", classifier.classify(InstanceRequest.class));
    }

    @Test
    void testCommonModule() {
        assertEquals("common", classifier.classify(ConnectionSetupRequest.class));
        assertEquals("common", classifier.classify(ErrorResponse.class));
    }
}
