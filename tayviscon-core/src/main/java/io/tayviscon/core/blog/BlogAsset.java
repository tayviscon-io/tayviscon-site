package io.tayviscon.core.blog;

/** Байты ассета статьи и MIME-тип. */
public record BlogAsset(byte[] bytes, String contentType) {}
