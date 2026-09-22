package io.tayviscon.core.course;

/** Карточка курса в каталоге. */
public record Course(
    String id,
    String title,
    String summary,
    CourseStatus status,
    String fallbackLetter,
    boolean hasLogo) {}
