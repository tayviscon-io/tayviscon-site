package io.tayviscon.core.course;

/** Байты логотипа курса вместе с HTTP-типом содержимого. */
public record LogoAsset(byte[] bytes, String contentType) {}
