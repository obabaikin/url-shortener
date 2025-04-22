package com.shortener.urlshortenerservice.generator;

import com.shortener.urlshortenerservice.model.HashItem;
import com.shortener.urlshortenerservice.repository.interfaces.HashRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class HashGenerator {

    private final HashRepository hashRepository;
    private final Base62Encoder base62Encoder;
    @Value("${hash.hash.capacity:10000}")
    private int capacity;

    public void generateHash() {
        log.info("Start generate hash number : {} ", capacity);
        List<Long> numbers = hashRepository.getUniqueNumbers(capacity);
        List<HashItem> hashes = base62Encoder.encodeList(numbers)
                .stream()
                .map(HashItem::new)
                .toList();
        hashRepository.saveAll(hashes);
    }

    public List<String> getHashes(long amount) {
        log.info("Get hash number : {} ", amount);
        List<HashItem> hashItems = hashRepository.getHashesAndDelete(amount);
        if (hashItems.size() < amount) {
            log.info("There are not enough rows in a hash table: {} ", amount - hashItems.size());
            generateHash();
            hashItems.addAll(hashRepository.getHashesAndDelete(amount - hashItems.size()));
        }
        return hashItems.stream().map(HashItem::getHash).toList();
    }

    @Async("hashGeneratorExecutor")
    public CompletableFuture<List<String>> getHashesAsync(long number) {
        log.error("Start Async get hashes. Numbers: {}", number);
        return CompletableFuture.completedFuture(getHashes(number));
    }
}