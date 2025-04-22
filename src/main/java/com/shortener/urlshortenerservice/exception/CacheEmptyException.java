package com.shortener.urlshortenerservice.exception;

public class CacheEmptyException extends RuntimeException {
    public CacheEmptyException(String message) {
        super(message);
    }
}