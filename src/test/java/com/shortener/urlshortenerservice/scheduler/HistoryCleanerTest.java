package com.shortener.urlshortenerservice.scheduler;

import com.shortener.urlshortenerservice.repository.interfaces.HashRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HistoryCleanerTest {
    @Mock
    private HashRepository hashRepository;
    @InjectMocks
    private HistoryCleaner historyCleaner;

    @ParameterizedTest
    @MethodSource("startJobTestCases")
    void startJobSuccessTest(boolean shouldThrowException) {
        if (shouldThrowException) {
            doThrow(new RuntimeException("Error. Exception.")).when(hashRepository).cleanDataOlder1Year();
        }

        historyCleaner.startJob();
        verify(hashRepository, times(1)).cleanDataOlder1Year();
    }

    private static Stream<Arguments> startJobTestCases() {
        return Stream.of(
                Arguments.of(false),
                Arguments.of(true)
        );
    }
}