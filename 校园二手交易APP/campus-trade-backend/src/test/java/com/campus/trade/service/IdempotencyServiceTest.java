package com.campus.trade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campus.trade.exception.BusinessException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.campus.trade.config.TestCacheConfig;

@SpringBootTest(classes = TestCacheConfig.class)
@ActiveProfiles("test")
class IdempotencyServiceTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Test
    void returnsCachedResponseWhenKeyReused() {
        AtomicInteger counter = new AtomicInteger();
        String owner = "user-1";
        String scope = "TEST";
        String key = "key-123";

        String first = idempotencyService.execute(key, owner, scope, Map.of("value", 1), () -> {
            counter.incrementAndGet();
            return "OK";
        }, String.class);

        String second = idempotencyService.execute(key, owner, scope, Map.of("value", 1), () -> {
            counter.incrementAndGet();
            return "SHOULD_NOT_EXECUTE";
        }, String.class);

        assertEquals("OK", first);
        assertEquals("OK", second);
        assertEquals(1, counter.get(), "Supplier should only run once");
    }

    @Test
    void throwsWhenKeyMissing() {
        assertThrows(BusinessException.class, () ->
                idempotencyService.execute(null, "user-1", "TEST", Map.of(), () -> "ANY", String.class));
    }
}
