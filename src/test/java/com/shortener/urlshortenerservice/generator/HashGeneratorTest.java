package com.shortener.urlshortenerservice.generator;

import com.shortener.urlshortenerservice.model.HashItem;
import com.shortener.urlshortenerservice.repository.interfaces.HashRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashGeneratorTest {
    @Mock
    private HashRepository hashRepository;
    @Mock
    private Base62Encoder base62Encoder;
    @InjectMocks
    private HashGenerator hashGenerator;
    private int capacity;
    private List<Long> numbers;
    private List<String> hashes;
    private List<String> strings;

    @BeforeEach
    void setUp() {
        capacity = 5;
        numbers = List.of(1L, 2L, 3L, 4L, 5L);
        hashes = List.of("1a", "2b", "3c", "4d", "5e");
        strings = List.of("1a", "2b", "3c", "4d", "5e");

        ReflectionTestUtils.setField(hashGenerator, "capacity", capacity);
    }

    @Test
    void generateHashSuccessTest() {
        when(hashRepository.getUniqueNumbers(capacity)).thenReturn(numbers);
        when(base62Encoder.encodeList(numbers)).thenReturn(hashes);

        hashGenerator.generateHash();
        verify(hashRepository, times(1)).saveAll(any());
    }

    @Test
    void getHashesSuccessTest() {
        int testNumberElements = 5;
        int wasElementsInHashTable = 2;

        ArrayList<HashItem> arrayList2 = new ArrayList<>();
        arrayList2.add(new HashItem(strings.get(0)));
        arrayList2.add(new HashItem(strings.get(1)));

        ArrayList<HashItem> arrayList3 = new ArrayList<>();
        arrayList3.add(new HashItem(strings.get(2)));
        arrayList3.add(new HashItem(strings.get(3)));
        arrayList3.add(new HashItem(strings.get(4)));


        when(hashRepository.getHashesAndDelete(testNumberElements)).thenReturn(arrayList2);
        when(hashRepository.getUniqueNumbers(capacity)).thenReturn(numbers);
        when(base62Encoder.encodeList(numbers)).thenReturn(hashes);
        when(hashRepository.getHashesAndDelete(capacity - wasElementsInHashTable)).thenReturn(arrayList3);

        List<String> stringsResult = hashGenerator.getHashes(testNumberElements);

        assertEquals(strings.size(), stringsResult.size(), "Size of the result list is not correct");
        strings.forEach(string -> assertTrue(stringsResult.contains(string), "Result list does not contain the element"));
    }

    @Test
    void getHashesAsyncSuccessTest() throws ExecutionException, InterruptedException, TimeoutException {
        int testNumberElements = 5;

        when(hashRepository.getHashesAndDelete(testNumberElements)).
                thenReturn(List.of(new HashItem(strings.get(0)),
                        new HashItem(strings.get(1)),
                        new HashItem(strings.get(2)),
                        new HashItem(strings.get(3)),
                        new HashItem(strings.get(4))));

        List<String> stringsResult = hashGenerator.getHashesAsync(testNumberElements).
                get(1000L, TimeUnit.MILLISECONDS);

        assertEquals(testNumberElements, stringsResult.size(), "Size of the result list is not correct");
        assertEquals(testNumberElements, strings.size(), "Size of strings is not correct");
        strings.forEach(hash -> assertTrue(stringsResult.contains(hash), "Result list does not contain the element"));
    }
}