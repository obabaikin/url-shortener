package com.shortener.urlshortenerservice.repository;

import com.shortener.urlshortenerservice.model.Urls;
import com.shortener.urlshortenerservice.repository.interfaces.UrlsJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class UrlsRepository {
    private final UrlsJpaRepository urlsJpaRepository;
    private final MessageSource messageSource;

    public Urls findByHash(String hash) {
        return urlsJpaRepository.findByHash(hash).orElseThrow(() ->
                new EntityNotFoundException(
                        messageSource.getMessage("exception.entity.not.found.text",
                                new Object[]{hash},
                                LocaleContextHolder.getLocale())));
    }
}