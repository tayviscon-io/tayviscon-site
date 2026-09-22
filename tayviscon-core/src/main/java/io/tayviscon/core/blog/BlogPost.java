package io.tayviscon.core.blog;

/** Краткая карточка поста для ленты на главной. */
public record BlogPost(String title, String summary, String date, String path) {}
