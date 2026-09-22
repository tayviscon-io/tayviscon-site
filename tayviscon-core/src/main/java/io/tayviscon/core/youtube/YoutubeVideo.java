package io.tayviscon.core.youtube;

/** Одно видео из Atom-ленты канала. */
public record YoutubeVideo(
    String id, String title, String url, String published, String thumbnailUrl) {}
