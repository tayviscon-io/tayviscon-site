package io.tayviscon.core.blog;

/** Опубликованная статья блога с HTML-фрагментом. */
public record BlogArticle(String title, String summary, String date, String path, String html) {}
