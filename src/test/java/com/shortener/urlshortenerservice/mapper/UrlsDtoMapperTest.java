package com.shortener.urlshortenerservice.mapper;

import com.shortener.urlshortenerservice.dto.UrlDto;
import com.shortener.urlshortenerservice.model.Urls;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlsDtoMapperTest {
    private final UrlsDtoMapper urlsDtoMapper = new UrlsDtoMapperImpl();

    @ParameterizedTest
    @MethodSource("provideUrlsForMapping")
    void toUrlDtoLongUrlSuccessTest(String inputUrl, String inputHash) {

        Urls input = Urls.builder()
                .url(inputUrl)
                .hash(inputHash)
                .build();

        UrlDto expected = UrlDto.builder()
                .url(inputUrl)
                .build();

        UrlDto actual = urlsDtoMapper.toUrlDtoLongUrl(input);

        assertEquals(expected, actual, "The method toUrlDtoLongUrl() is not correct.");
    }

    private static Stream<Arguments> provideUrlsForMapping() {
        return Stream.of(
                Arguments.of("https://example.com/page", "abc123"),
                Arguments.of("http://test.com", "xyz789"),
                Arguments.of("", "emptyHash"),
                Arguments.of(null, "nullUrl")
        );
    }
}