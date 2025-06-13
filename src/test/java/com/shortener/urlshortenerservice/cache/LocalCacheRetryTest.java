package com.shortener.urlshortenerservice.cache;

import com.shortener.urlshortenerservice.exception.CacheEmptyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalCacheRetryTest {
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private LocalCacheRetry localCache;

    private final int  capacityTest = 4;

    @Test
    void getCachedHashSuccessTest() {
        String testHash = "a1b";
        Queue<String> hashes = new ArrayBlockingQueue<>(capacityTest);
        hashes.add(testHash);
        assertEquals(testHash, localCache.getCachedHash(hashes), "The hash is not equal.");
    }

    @Test
    void getCachedHashNoElementInQueueExceptionFailTest() {
        Queue<String> hashes = new ArrayBlockingQueue<>(capacityTest);
        String message = "There are a lot of requests. Please, try again later.";

        assertTrue(hashes.isEmpty(), "Queue should be empty before calling getCachedHash");

        when(messageSource.getMessage(eq("exception.cache.empty"), any(), any()))
                .thenReturn(message);

        Exception exception = assertThrows(CacheEmptyException.class, () -> localCache.getCachedHash(hashes));

        assertEquals(message, exception.getMessage(), "The message is not equal.");
    }
}