package com.shortener.urlshortenerservice.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class Base62EncoderTest {
    @InjectMocks
    private Base62Encoder base62Encoder;
    String solWordTest;
    int solWordLengthTest;

    @BeforeEach
    public void setUp() {
        solWordTest = "a1b";
        ReflectionTestUtils.setField(base62Encoder, "solWord", solWordTest);
        base62Encoder.init();
    }

    @Test
    void initSuccessTest() {
        solWordLengthTest = (int) ReflectionTestUtils.getField(base62Encoder, "solWordLength");
        assertEquals(solWordTest.length(), solWordLengthTest, "Check solWordLength");
    }

    @Test
    void encodeListSuccessTest() {
        List<Long> longList = List.of(100L, 102L, 105L);
        List<String> hashListResult = base62Encoder.encodeList(longList);

        assertEquals(longList.size(), hashListResult.size(), "Size mismatch");


        hashListResult.forEach(hash -> {
            assertFalse(hash.isEmpty(),"Check that hash is not empty");
            boolean isValid = hash.chars().allMatch(c -> solWordTest.indexOf(c) >= 0);
            assertTrue(isValid, "Hash contains invalid characters, expected only characters from solWord");
        });
    }

    @Test
    void encodeListNullExceptionSuccessTest() {
        List<String> hashListResult = base62Encoder.encodeList(null);
        assertEquals(0, hashListResult.size(), "Check null List");
    }
}