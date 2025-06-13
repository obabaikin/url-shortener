package com.shortener.urlshortenerservice.generator.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.shortener.urlshortenerservice.controller.UrlShortenerController;
import com.shortener.urlshortenerservice.dto.UrlDto;
import com.shortener.urlshortenerservice.locale.LocaleChangeFilter;
import com.shortener.urlshortenerservice.locale.handler.ExceptionApiHandler;
import com.shortener.urlshortenerservice.model.Urls;
import com.shortener.urlshortenerservice.repository.RedisRepository;
import com.shortener.urlshortenerservice.repository.interfaces.UrlsJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

import static com.shortener.urlshortenerservice.testutils.TestMessage.END_MESSAGE;
import static com.shortener.urlshortenerservice.testutils.TestMessage.ERROR_URL_MESSAGE;
import static com.shortener.urlshortenerservice.testutils.TestMessage.GENERATED_SHORT_URL_MESSAGE;
import static com.shortener.urlshortenerservice.testutils.TestMessage.HASH_MESSAGE;
import static com.shortener.urlshortenerservice.testutils.TestMessage.START_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UrlShortenerControllerIT {
    @Autowired
    private UrlShortenerController urlShortenerController;
    @Autowired
    private RedisRepository redisRepository;
    @Autowired
    private UrlsJpaRepository urlsJpaRepository;
    @InjectMocks
    private LocaleChangeFilter localeChangeFilter;

    private MockMvc mockMvc;

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerControllerIT.class);

    @Value("${hash.app.url-name}")
    private String expectedUrl;

    private String urlController;
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Container
    public static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:13.6");

    @Container
    public static final RedisContainer REDIS_CONTAINER =
            new RedisContainer(DockerImageName.parse("redis/redis-stack:latest"))
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);

        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    }

    @BeforeEach
    void setUp() {
        urlController = "/api/url_shortener/v1/url";
        mockMvc = MockMvcBuilders
                .standaloneSetup(urlShortenerController)
                .setControllerAdvice(new ExceptionApiHandler())
                .addFilter(localeChangeFilter)
                .build();
    }

    @ParameterizedTest(name = "Success test #{index} - {0}")
    @MethodSource("validLongUrls")
    void getShortUrlSuccessTest(String originalUrl) throws Exception {
        logStart(String.format("getShortUrlSuccessTest(%s)", originalUrl));

        UrlDto longUrlDto = new UrlDto(originalUrl);

        MvcResult urlDtoResult = mockMvc.perform(post(urlController)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(longUrlDto))
                        .header("Accept-Language", "en-US"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        log.info(urlDtoResult.getResponse().getContentAsString());
        UrlDto urlDto = OBJECT_MAPPER.readValue(urlDtoResult.getResponse().getContentAsString(), UrlDto.class);
        assertTrue(urlDto.url().contains(expectedUrl), "The response does not contain the expected url");
        String shortUrlTest = urlDto.url();

        log.info("shortUrl: " + shortUrlTest);

        String hashTest = getHashFromShortUrl(shortUrlTest);

        String urlRedisResult = redisRepository.getUrl(hashTest);
        log.info("Url from Redis: " + urlRedisResult);

        Urls urlsResult = urlsJpaRepository.findByHash(hashTest).orElseThrow(() -> new IllegalStateException("URL not found in DB"));
        log.info("Url from DB: " + urlsResult.getUrl());

        assertEquals(originalUrl, urlRedisResult, "The url from Redis is not as expected");
        assertEquals(originalUrl, urlsResult.getUrl(), "The url from DB is not as expected");
        logEnd(String.format("getShortUrlSuccessTest(%s)", originalUrl));
    }

    @ParameterizedTest(name = "Success test #{index} - {0}")
    @MethodSource("validLongUrls")
    void getLongUrlSuccessTest(String longUrlTest) throws Exception {
        logStart(String.format("getLongUrlSuccessTest(%s)", longUrlTest));

        UrlDto shortUrlDto = new UrlDto(createShortUrl(longUrlTest));

        MvcResult urlDtoResult = mockMvc.perform(get(urlController)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(shortUrlDto))
                        .header("Accept-Language", "dn"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        log.info(urlDtoResult.getResponse().getContentAsString());

        UrlDto urlDto = OBJECT_MAPPER.readValue(urlDtoResult.getResponse().getContentAsString(), UrlDto.class);

        assertTrue(urlDto.url().contains(longUrlTest), "The response does not contain the expected url");
        logEnd(String.format("getLongUrlSuccessTest(%s)", longUrlTest));
    }

    @ParameterizedTest(name = "Success test #{index} - {0}")
    @MethodSource("validLongUrls")
    void getLongUrlByHashSuccessTest(String longUrlTest) throws Exception {
        logStart(String.format("getLongUrlByHashSuccessTest(%s)", longUrlTest));

        String shortUrlTest = createShortUrl(longUrlTest);
        String hashTest = getHashFromShortUrl(shortUrlTest);

        MvcResult mvcResult = mockMvc.perform(get(urlController + "/" + hashTest))
                .andDo(print())
                .andExpect(status().isFound())
                .andReturn();

        log.info("Url header Location: {}", mvcResult.getResponse().getHeader("Location"));
        assertTrue(Objects.requireNonNull(mvcResult.getResponse().getHeader("Location")).contains(longUrlTest),
                "The response does not contain the expected url");
        logEnd(String.format("getLongUrlByHashSuccessTest(%s)", longUrlTest));
    }

    @ParameterizedTest(name = "Invalid hash test #{index} - {0}")
    @CsvSource({
            "12_test_length_hash, BAD_REQUEST",
            "ab23&_, NOT_FOUND"
    })
    void invalidHashShouldFailTest(String errorHashTest, String expectedStatus) throws Exception {

        logStart(String.format("invalidHashShouldFailTest( errorHashTest = %s , expectedStatus = %s", errorHashTest, expectedStatus));

        mockMvc.perform(get(urlController + "/" + errorHashTest))
                .andDo(print())
                .andExpect(status().is(expectedStatus.equals("BAD_REQUEST") ? 400 : 404));

        logEnd(String.format("invalidHashShouldFailTest( errorHashTest = %s , expectedStatus = %s", errorHashTest, expectedStatus));
    }

    @ParameterizedTest(name = "Invalid long URL test #{index} - {0}")
    @ValueSource(strings = {
            "http://www.test-urlshortener.com\\long-url/v1/there-is-a-long-url-here",
            "http://www.test-urlshortener.com\\22",
            "invalid-url"
    })
    void invalidLongUrlShouldFailTest(String wrongLongUrl) throws Exception {
        logStart(String.format( "invalidLongUrlShouldFailTest( %s )", wrongLongUrl ));

        UrlDto longUrlDto = new UrlDto(wrongLongUrl);

        mockMvc.perform(post(urlController)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(longUrlDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(String.format(ERROR_URL_MESSAGE, wrongLongUrl)));

        logEnd(String.format( "invalidLongUrlShouldFailTest( %s )", wrongLongUrl ));
    }

    @ParameterizedTest(name = "Invalid short URL test #{index} - {0}")
    @ValueSource(strings = {
            "http://www.test-urlshortener.com\\22",
            "invalid-url",
            "http:/incomplete.com",
            "http:// spaces.com",
            "http://wrong[char].com"
    })
    void isValidShortUrlIsValidLongUrlFailTest(String wrongShortUrl) throws Exception {
        logStart(String.format("getLongUrlIsValidLongUrlFailTest( %s )", wrongShortUrl));

        UrlDto shortUrlDto = new UrlDto(wrongShortUrl);

        mockMvc.perform(get(urlController)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(shortUrlDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(String.format(ERROR_URL_MESSAGE, wrongShortUrl)));

        logEnd(String.format("getLongUrlIsValidLongUrlFailTest( %s )", wrongShortUrl));
    }

    private void logStart(String testName) {
        log.info(START_MESSAGE, testName);
    }

    private void logEnd(String testName) {
        log.info(END_MESSAGE, testName);
    }

    private String createShortUrl(String longUrlTest) throws Exception {

        UrlDto longUrlDto = new UrlDto(longUrlTest);

        MvcResult postResult = mockMvc.perform(post(urlController)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(longUrlDto))
                        .header("Accept-Language", "en-US"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        UrlDto postResponseDto = OBJECT_MAPPER.readValue(postResult.getResponse().getContentAsString(), UrlDto.class);
        String shortUrl = postResponseDto.url();
        log.info(GENERATED_SHORT_URL_MESSAGE, shortUrl);

        return shortUrl;
    }

    private String getHashFromShortUrl(String shortUrlTest) {
        String hashTest = shortUrlTest.replaceAll(".*/([^/]+)$", "$1");
        log.info(HASH_MESSAGE + hashTest);

        return hashTest;
    }

    private static Stream<String> validLongUrls() {
        return Stream.of(
                "http://www.test-urlshortener.com/long-url/v1/there-is-a-long-url-here",
                "https://example.com/one",
                "http://test.org/path/to/page",
                "https://sub.domain.com/param?query=123"
        );
    }
}