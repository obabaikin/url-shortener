package com.shortener.urlshortenerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortener.urlshortenerservice.dto.UrlDto;
import com.shortener.urlshortenerservice.locale.handler.ExceptionApiHandler;
import com.shortener.urlshortenerservice.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UrlShortenerControllerTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mockMvc;

    @Mock
    private UrlShortenerService urlShortenerService;
    @Mock
    private MessageSource messageSource;
    @InjectMocks
    private UrlShortenerController urlShortenerController;
    private String urlController;

    @BeforeEach
    void setUp() {
        urlController = "/api/url_shortener/v1/url";
        String urlName = "http://test-shortner-service.com/";
        int maxHashLength = 6;

        ReflectionTestUtils.setField(urlShortenerController, "urlName", urlName);
        ReflectionTestUtils.setField(urlShortenerController, "maxHashLength", maxHashLength);

        mockMvc = MockMvcBuilders
                .standaloneSetup(urlShortenerController)
                .setControllerAdvice(new ExceptionApiHandler())
                .build();

        urlShortenerController.init();
    }

    @ParameterizedTest
    @MethodSource("validShortUrlProvider")
    void getShortUrlSuccessTest(String inputLongUrl, String expectedShortUrl) throws Exception {
        UrlDto longUrlDto = new UrlDto(inputLongUrl);
        UrlDto expectedUrlDto = new UrlDto(expectedShortUrl);

        when(urlShortenerService.getShortUrl(anyString())).thenReturn(expectedUrlDto);

        mockMvc.perform(post(urlController)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(longUrlDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedUrlDto.url()));
    }

    @ParameterizedTest
    @MethodSource("invalidUrlProvider")
    void shouldReturnBadRequestForInvalidUrlsFailTest(UrlDto invalidUrlDto) throws Exception {

        mockMvc.perform(post(urlController)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(invalidUrlDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("validLongUrlProvider")
    void getLongUrlSuccessTest(String shortUrl, String expectedLongUrl, String hash) throws Exception {

        UrlDto shortUrlDto = new UrlDto(shortUrl);
        UrlDto expectedUrlDto = new UrlDto(expectedLongUrl);

        when(urlShortenerService.getLongUrl(hash)).thenReturn(expectedUrlDto);

        mockMvc.perform(get(urlController)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(shortUrlDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedUrlDto.url()));
    }

    @ParameterizedTest
    @MethodSource("validLongUrlByHashProvider")
    void getLongUrlByHashSuccessTest(String hash, String expectedLongUrl) throws Exception {
        UrlDto expectedUrlDto = new UrlDto(expectedLongUrl);

        when(urlShortenerService.getLongUrl(hash)).thenReturn(expectedUrlDto);

        mockMvc.perform(get(urlController + "/" + hash))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", expectedLongUrl));
    }

    private static Stream<UrlDto> invalidUrlProvider() {
        return Stream.of(
                new UrlDto(""),
                new UrlDto("not-a-valid-url"));
    }

    private static Stream<Arguments> validShortUrlProvider() {
        return Stream.of(
                Arguments.of("http://www.test-urlshortener.com/long-url/v1/there-is-a-long-url-here", "http://test-shortner-service.com/abc123"),
                Arguments.of("https://another-site.com/page", "http://test-shortner-service.com/xyz789")
        );
    }

    private static Stream<Arguments> validLongUrlProvider() {
        return Stream.of(
                Arguments.of("http://test-shortner-service.com/a1b1", "http://www.test-urlshortener.com/long-url/v1/there-is-a-long-url-here", "a1b1"),
                Arguments.of("http://test-shortner-service.com/b2c2", "https://another-site.com/page", "b2c2")
        );
    }

    private static Stream<Arguments> validLongUrlByHashProvider() {
        return Stream.of(
                Arguments.of("a1b1", "http://www.test-urlshortener.com/long-url/v1/there-is-a-long-url-here"),
                Arguments.of("b2c2", "https://another-site.com/page")
        );
    }
}