package com.shortener.urlshortenerservice.dto;

import lombok.Builder;

@Builder
public record UrlDto(String url) {
}