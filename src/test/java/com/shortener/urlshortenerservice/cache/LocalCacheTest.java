package com.shortener.urlshortenerservice.cache;

import com.shortener.urlshortenerservice.generator.HashGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalCacheTest {
    @Mock
    private HashGenerator hashGenerator;
    @Mock
    private LocalCacheRetry localCacheRetry;

    @InjectMocks
    private LocalCache localCache;

    private List<String> listTestHash;
    private Queue<String> testHashes;
    private final int capacity = 5;
    private static final int DEFAULT_FILL_PERCENT = 60;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(localCache, "capacity", capacity);
        ReflectionTestUtils.setField(localCache, "fillPercent", DEFAULT_FILL_PERCENT);
    }

    @Test
    void initSuccessTest() {
        listTestHash = List.of("123", "234", "345", "qwe");

        int expectedTestMinQueueSize = 3;

        initWithHashes(listTestHash);

        int testMinQueueSize = getMinQueueSize();
        testHashes = getHashesQueue();

        assertEquals(expectedTestMinQueueSize, testMinQueueSize, " Check minQueueSize");
        assertNotNull(testHashes, "Queue<String> should not be null");
        assertEquals(listTestHash.size(), testHashes.size(), "Queue size mismatch");

        listTestHash.forEach(s ->
                assertEquals(s, testHashes.poll(), " Check element in Queue<String>"));

        assertTrue(testHashes.isEmpty(), "Queue<String> should be empty after polling all elements");
    }

    @Test
    void initDoesNotTerminateOnExceptionSuccessTest() {
        when(hashGenerator.getHashes(capacity)).thenThrow(new IllegalStateException("Queue is full"));

        assertDoesNotThrow(() -> localCache.init());
        testHashes = getHashesQueue();
        int testMinQueueSize = getMinQueueSize();

        assertNotNull(testHashes, "Queue<String> should not be null");
        assertTrue(testHashes.isEmpty(), "Queue<String> should be empty");
        assertEquals(capacity * DEFAULT_FILL_PERCENT / 100, testMinQueueSize, "minQueueSize is not set correctly");
    }

    @Test
    void getHashFillQueueSuccessTest() {
        listTestHash = List.of("123", "234", "345", "456", "567");

        initWithHashes(listTestHash);

        String testResult = localCache.getHash();

        assertEquals(listTestHash.get(0), testResult, "The result not equal: getHash() == listTestHash.get(0)");

        testHashes = getHashesQueue();
        assertEquals(capacity - 1, testHashes.size(), "testHashes.size() !=  (capacity - 1)");
    }

    @Test
    void getHashNewItemsWereAddedToQueueSuccessTest() {
        listTestHash = List.of("123", "234");
        CompletableFuture<List<String>> listCompletableFuture =
                CompletableFuture.completedFuture(List.of("323", "423", "523"));
        int neededNumberOfItems = 3;

        when(hashGenerator.getHashes(capacity)).thenReturn(listTestHash);
        when(hashGenerator.getHashesAsync(neededNumberOfItems)).thenReturn(listCompletableFuture);

        localCache.init();
        String testResult = localCache.getHash();

        assertEquals(listTestHash.get(0), testResult, "The result not equal: getHash() == listTestHash.get(0)");

        testHashes = getHashesQueue();
        assertEquals(capacity - 1, testHashes.size(), "Expected queue size to be 4 (capacity - 1), but was " + testHashes.size());
    }

    @Test
    void shouldReturnSingleHashWhenFillingFlagIsTrueSuccessTest() {
        listTestHash = List.of("123");
        AtomicBoolean fillingTest = new AtomicBoolean(true);
        ReflectionTestUtils.setField(localCache, "filling", fillingTest);

        initWithHashes(listTestHash);

        String testResult = localCache.getHash();
        assertEquals(listTestHash.get(0), testResult, "The result not equal: getHash() == listTestHash.get(0)");

        testHashes = getHashesQueue();
        AtomicBoolean fillingTestResult = getFillingFlag();

        assertEquals(0, testHashes.size(), "testHashes.size() !=  0");
        assertTrue(fillingTestResult.get(), "AtomicBoolean filling should be True");
    }

    @Test
    void getHashQueueHas0ItemTrueSuccessTest() {
        String testHash = "a1b";
        listTestHash = List.of();
        AtomicBoolean fillingTest = new AtomicBoolean(true);
        ReflectionTestUtils.setField(localCache, "filling", fillingTest);

        initWithHashes(listTestHash);

        testHashes = getHashesQueue();
        when(localCacheRetry.getCachedHash(testHashes)).thenReturn(testHash);

        String testResult = localCache.getHash();
        assertEquals(testHash, testResult, "The result not equal: getHash() == listTestHash.get(0)");

        testHashes = getHashesQueue();
        AtomicBoolean fillingTestResult = getFillingFlag();

        assertEquals(0, testHashes.size(), "testHashes.size() !=  0");
        assertTrue(fillingTestResult.get(), "AtomicBoolean filling should be True");
    }

    private void initWithHashes(List<String> hashes) {
        when(hashGenerator.getHashes(capacity)).thenReturn(hashes);
        localCache.init();
    }

    @SuppressWarnings("unchecked")
    private Queue<String> getHashesQueue() {
        return (Queue<String>) ReflectionTestUtils.getField(localCache, "hashes");
    }

    private int getMinQueueSize() {
        Object value = ReflectionTestUtils.getField(localCache, "minQueueSize");
        if (value == null) {
            throw new IllegalStateException("LocalCache is not initialized.");
        }
        return (int) value;
    }

    private AtomicBoolean getFillingFlag() {
        Object value = ReflectionTestUtils.getField(localCache, "filling");
        if (!(value instanceof AtomicBoolean)) {
            throw new IllegalStateException("Filling flag is not initialized or not an instance of AtomicBoolean.");
        }
        return (AtomicBoolean) value;
    }
}