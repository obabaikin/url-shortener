package com.shortener.urlshortenerservice.mapper;

import com.shortener.urlshortenerservice.dto.UrlDto;
import com.shortener.urlshortenerservice.dto.UrlsDto;
import com.shortener.urlshortenerservice.model.Urls;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UrlsDtoMapper {
    Urls toUrls(UrlsDto urlsDto);

    @Mapping(source = "url", target = "url")
    UrlDto toUrlDtoLongUrl(Urls urls);
}